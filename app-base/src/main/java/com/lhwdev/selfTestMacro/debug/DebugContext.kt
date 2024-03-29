package com.lhwdev.selfTestMacro.debug

import android.util.Log
import com.lhwdev.io.runInterruptibleGracefully
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.debuggingWithIde
import com.lhwdev.utils.rethrowIfNeeded
import kotlinx.coroutines.*
import java.io.File


private inline fun <T> T?.merge(other: T?, merger: (T, T) -> T): T? = when {
	this == null -> other
	other == null -> this
	this == other -> this
	else -> merger(this, other)
}


var sIncludeLogcatInLog = false


class ErrorInfo(
	val message: String,
	val throwable: Throwable? = null,
	val diagnostics: List<DiagnosticObject> = emptyList(),
	val location: String = invokeLocationDescription(depth = 1),
	val severity: DebugContext.Severity
) {
	val allDiagnostics: List<DiagnosticObject>
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
	
	
	/**
	 * @param message **User-friendly** message which is properly localized.
	 */
	inline fun onError(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticObject> = emptyList(),
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
		diagnostics: List<DiagnosticObject> = emptyList(),
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
		error.throwable?.rethrowIfNeeded()
		
		manager.workScope.launch {
			onLightErrorSuspend(error, shortLog)
		}
	}
	
	suspend fun onLightErrorSuspend(error: ErrorInfo, shortLog: Boolean) {
		error.throwable?.rethrowIfNeeded()
		
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
	
	abstract fun childContext(hint: String): DebugContext
	
	
	// Internals
	
	suspend fun showErrorInfo(
		info: ErrorInfo,
		description: String
	) {
		try {
			onShowErrorInfo(info, description)
		} catch(th: Throwable) {
			th.rethrowIfNeeded()
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
		${diagnostics.joinToString(separator = "\n") { it.dumpDebug(oneLine = false) }}
		
		Logcat:
		${getLogcat()}
		
		------------------------------------------------------------------------------------------------
		
	""".trimIndent()
	
	private suspend fun getLogcat(): String = if(sIncludeLogcatInLog) runInterruptibleGracefully(Dispatchers.IO) {
		val command = arrayOf("logcat", "-d", "-v", "threadtime")
		
		@Suppress("BlockingMethodInNonBlockingContext")
		val process = Runtime.getRuntime().exec(command)
		process.inputStream.reader().use { it.readText() }
	} else "(no logcat)"
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
	
	override fun childContext(hint: String): DebugContext = SimpleDebugContext(flags, manager, "$contextName/$hint")
}

private class SimpleDebugContext(
	flags: DebugFlags,
	manager: DebugManager,
	override val contextName: String
) : DebugContext(flags, manager) {
	
	override fun childContext(hint: String) = SimpleDebugContext(flags, manager, "$contextName/$hint")
	
	override suspend fun onShowErrorInfo(info: ErrorInfo, description: String) {}
}
