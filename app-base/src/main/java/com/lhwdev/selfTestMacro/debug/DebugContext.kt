package com.lhwdev.selfTestMacro.debug

import android.util.Log
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.debuggingWithIde
import kotlinx.coroutines.*
import java.io.File


private inline fun <T> T?.merge(other: T?, merger: (T, T) -> T): T? = when {
	this == null -> other
	other == null -> this
	this == other -> this
	else -> merger(this, other)
}


class ErrorInfo(
	val message: String,
	val throwable: Throwable?,
	val diagnostics: List<DiagnosticItem> = emptyList(),
	val location: String = invokeLocationDescription(depth = 1),
	val severity: DebugContext.Severity
) {
	val allDiagnostics: List<DiagnosticItem>
		get() = if(throwable is DiagnosticObject) {
			diagnostics + throwable.getDiagnosticInformation()
		} else {
			diagnostics
		}
	
	fun merge(other: ErrorInfo): ErrorInfo = ErrorInfo(
		message = "(merged) $message // ${other.message}",
		throwable = throwable.merge(other.throwable) { a, b -> a.addSuppressed(b); a },
		diagnostics = diagnostics + other.diagnostics,
		location = if(location == other.location) location else "(merged) $location, ${other.location}",
		severity = if(severity > other.severity) severity else other.severity
	)
}


private val sTraceItemAnnotation = TraceItem::class.java
private val sTraceItemsAnnotation = TraceItems::class.java


/**
 * DebugContext is a cheap class which describes 'where is this'.
 */
@Suppress("NOTHING_TO_INLINE")
abstract class DebugContext(
	val flags: DebugFlags,
	val manager: DebugManager
) {
	data class DebugFlags(
		val enabled: Boolean,
		val debuggingWithIde: Boolean
	)
	
	enum class Severity { light, significant, critical }
	
	
	abstract val contextName: String
	
	
	inline fun onError(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.significant
	) {
		onError(ErrorInfo(message, throwable, diagnostics, location, severity), forceShow = forceShow)
	}
	
	fun onError(error: ErrorInfo, forceShow: Boolean = false) {
		manager.onErrorFromContext(this, error, forceShow)
	}
	
	// prevent duplicate error
	inline fun onThrowError(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.significant
	) {
		onThrowError(ErrorInfo(message, throwable, diagnostics, location, severity), forceShow = forceShow)
	}
	
	fun onThrowError(error: ErrorInfo, forceShow: Boolean = false) {
		manager.pendThrowingError(DebugManager.PendingError(context = this, error = error))
	}
	
	
	suspend fun onErrorSuspend(error: ErrorInfo, forceShow: Boolean = false) {
		Log.e("ERROR", "(${error.location}) ${error.message}", error.throwable)
		val info = error.getInfo()
		if(forceShow || flags.enabled) {
			showErrorInfo(error, info)
		}
		
		// in dev mode, usually ran with IDE; can check things with Logcat
		if(!flags.debuggingWithIde) {
			writeErrorLog(info)
		}
	}
	
	
	inline fun onLightError(
		message: String,
		throwable: Throwable? = null,
		diagnostics: List<DiagnosticItem> = emptyList(),
		shortLog: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.light
	) {
		onLightError(ErrorInfo(message, throwable, diagnostics, location, severity), shortLog = shortLog)
	}
	
	fun onLightError(error: ErrorInfo, shortLog: Boolean = false) {
		manager.workScope.launch {
			onLightErrorSuspend(error, shortLog)
		}
	}
	
	suspend fun onLightErrorSuspend(error: ErrorInfo, shortLog: Boolean) {
		Log.e("ERROR", "(${error.location}) ${error.message}", error.throwable)
		
		val showInfo = error.severity > Severity.light
		
		val info = if(flags.debuggingWithIde && showInfo) {
			"(stub)" // Stub
		} else if(shortLog) {
			"[SelfTestMacro ${App.version}]"
		} else {
			error.getInfo()
		}
		
		if(showInfo) {
			showErrorInfo(error, info)
		}
		
		// in dev mode, usually ran with IDE; can check things with Logcat
		if(!flags.debuggingWithIde) {
			writeErrorLog(info)
		}
	}
	
	
	// Internals
	
	private suspend fun showErrorInfo(
		info: ErrorInfo,
		description: String
	) {
		try {
			onShowErrorInfo(info, description)
		} catch(th: Throwable) {
			onLightError("showErrorInfo failed", throwable = th)
		}
	}
	
	protected abstract suspend fun onShowErrorInfo(info: ErrorInfo, description: String)
	
	
	private suspend fun writeErrorLog(info: String) {
		try {
			withContext(Dispatchers.IO) {
				File(manager.debugLogDirectory, "error_log.txt").appendText(info)
			}
		} catch(e: Throwable) {
			// ignore errors
		}
	}
	
	
	private suspend fun ErrorInfo.getInfo() = """
		[SelfTestMacro ${App.version}] $contextName: $message
		Location: $location
		Android sdk version: ${android.os.Build.VERSION.SDK_INT}
		Model: ${android.os.Build.DEVICE} / ${android.os.Build.PRODUCT}
		Stacktrace:
		${throwable?.stackTraceToString()}
		
		Diagnostic:
		${diagnostics.joinToString(separator = "\n") { it.dump(oneLine = false) }}
		
		Logcat:
		${getLogcat()}
		
		------------------------------------------------------------------------------------------------
		
	""".trimIndent()
	
	private suspend fun getLogcat(): String = runInterruptible(Dispatchers.IO) {
		val command = arrayOf("logcat", "-d", "-v", "threadtime")
		
		@Suppress("BlockingMethodInNonBlockingContext")
		val process = Runtime.getRuntime().exec(command)
		process.inputStream.reader().use { it.readText() }
	}
}


object GlobalDebugContext : DebugContext(
	flags = DebugFlags(enabled = true, debuggingWithIde = App.debuggingWithIde),
	manager = object : DebugManager() {
		@OptIn(DelicateCoroutinesApi::class)
		override val workScope: CoroutineScope = GlobalScope
	}
) {
	override val contextName: String get() = "global"
	
	override suspend fun onShowErrorInfo(info: ErrorInfo, description: String) {
		// no-op; not available
	}
}
