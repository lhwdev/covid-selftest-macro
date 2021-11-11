package com.lhwdev.selfTestMacro.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Method


private val sTraceItemAnnotation = TraceItem::class.java


abstract class DebugManager {
	class PendingError(
		val context: DebugContext,
		val message: String,
		val throwable: Throwable?,
		val diagnostics: List<DiagnosticItem>,
		val forceShow: Boolean,
		val location: String,
		val severity: DebugContext.Severity = DebugContext.Severity.significant
	)
	
	
	abstract val androidContext: Context
	
	abstract val workScope: CoroutineScope
	
	protected val pendingErrors = mutableMapOf<Any?, Job>()
	
	
	open fun identifierOf(error: PendingError): Any? = error.throwable ?: Any() // Any(): not identifiable
	
	
	open fun pendThrowingError(error: PendingError) {
		pendError(afterMillis = 50, error = error)
	}
	
	open fun pendError(afterMillis: Int, error: PendingError) {
		workScope.launch {
			delay(afterMillis.toLong())
			
		}
	}
}


/**
 * DebugContext is a cheap class which describes 'where is this'.
 */
abstract class DebugContext(
	val flags: DebugFlags,
	val manager: DebugManager
) {
	data class DebugFlags(
		val enabled: Boolean,
		val debuggingWithIde: Boolean
	)
	
	enum class Severity { light, significant, critical }
	
	class ErrorInfo(
		val message: String,
		val detailedMessage: String,
		val throwable: Throwable?,
		val diagnostics: List<DiagnosticItem>,
		val severity: Severity
	)
	
	companion object {
		val ShowErrorDialog = { debugContext: ComposeDebugContext, errorInfo: ErrorInfo ->
			val context = debugContext.context
			AlertDialog.Builder(context).apply {
				setTitle("오류 발생")
				setMessage("* 복사된 오류정보는 기기의 정보 등 민감한 정보를 포함할 수 있어요.\n${errorInfo.detailedMessage}")
				
				setPositiveButton("오류정보 복사") { _, _ ->
					CoroutineScope(Dispatchers.Main).launch {
						context.getSystemService<ClipboardManager>()!!
							.setPrimaryClip(ClipData.newPlainText("오류정보", errorInfo.detailedMessage))
						context.showToastSuspendAsync("복사 완료")
					}
				}
				setNegativeButton("취소", null)
			}.show()
		}
	}
	
	
	abstract val contextName: String
	
	abstract val uiScope: CoroutineScope
	
	
	/**
	 * Getting invocation location is a quite slow job, getting all stacktrace and figuring out the original method.
	 * Should be only used in error.
	 */
	fun invokeLocationDescription(depth: Int): String {
		val method = invokeLocation(depth = depth + 1)
		
		return if(method == null) {
			"?"
		} else {
			"${method.declaringClass.name}.${method.name}"
		}
	}
	
	fun invokeLocation(depth: Int): Method? {
		val realDepth = depth + 1
		val trace = Throwable().stackTrace
		
		for(index in realDepth until trace.size) {
			val element = trace[index] ?: continue
			
			val method = methodFromStackTrace(element, checkAnnotation = true)
			if(method != null) {
				return method
			}
		}
		
		return methodFromStackTrace(trace[realDepth], checkAnnotation = false)
	}
	
	private fun methodFromStackTrace(element: StackTraceElement, checkAnnotation: Boolean): Method? = try {
		val targetClass = DebugContext::class.java.classLoader.loadClass(element.className)
		
		if(checkAnnotation) {
			targetClass.declaredMethods.find {
				it.name == element.methodName &&
					it.isAnnotationPresent(sTraceItemAnnotation)
			}
		} else {
			targetClass.declaredMethods.find { it.name == element.methodName }
		}
	} catch(th: Throwable) {
		null
	}
	
	
	fun onError(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.significant
	) {
		
		manager.workScope.launch {
			onErrorSuspend(message, throwable, diagnostics, forceShow, location)
		}
	}
	
	// prevent duplicate error
	fun onThrowError(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.significant
	) {
		manager.pendThrowingError(
			error = DebugManager.PendingError(
				context = this,
				message = message,
				throwable = throwable,
				diagnostics = diagnostics,
				forceShow = forceShow,
				location = location,
				severity = severity
			)
		)
	}
	
	
	
	suspend fun onErrorSuspend(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.significant
	) {
		Log.e("ERROR", "($location) $message", throwable)
		val info = getErrorInfo(throwable, message, diagnostics, location)
		if(forceShow || flags.enabled) {
			showErrorInfo(ErrorInfo(message, info, throwable, diagnostics, severity))
		}
		
		// in dev mode, usually ran with IDE; can check things with Logcat
		if(!flags.debuggingWithIde) {
			writeErrorLog(info)
		}
	}
	
	
	fun onLightError(
		message: String,
		throwable: Throwable? = null,
		diagnostics: List<DiagnosticItem> = emptyList(),
		shortLog: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.light
	) {
		manager.workScope.launch {
			onLightErrorSuspend(message, throwable, diagnostics, shortLog, location)
		}
	}
	
	suspend fun onLightErrorSuspend(
		message: String,
		throwable: Throwable? = null,
		diagnostics: List<DiagnosticItem> = emptyList(),
		shortLog: Boolean = false,
		location: String = invokeLocationDescription(depth = 1),
		severity: Severity = Severity.light
	) {
		Log.e("ERROR", "($location) $message", throwable)
		
		val showInfo = severity > Severity.light
		
		val info = if(flags.debuggingWithIde && showInfo) {
			"(stub)" // Stub
		} else if(shortLog) {
			"[SelfTestMacro ${App.version}]"
		} else {
			getErrorInfo(throwable, message, diagnostics, location)
		}
		
		if(showInfo) {
			showErrorInfo(ErrorInfo(message, info, throwable, diagnostics, severity))
		}
		
		// in dev mode, usually ran with IDE; can check things with Logcat
		if(!flags.debuggingWithIde) {
			writeErrorLog(info)
		}
	}
	
	
	private suspend fun showErrorInfo(info: ErrorInfo): Unit = withContext(uiScope.coroutineContext) {
		try {
			onShowErrorInfo(info)
		} catch(th: Throwable) {
			onLightError("showErrorInfo failed", throwable = th)
		}
	}
	
	protected abstract suspend fun onShowErrorInfo(info: ErrorInfo)
	
	
	private suspend fun writeErrorLog(info: String) {
		try {
			withContext(Dispatchers.IO) {
				File(manager.androidContext.getExternalFilesDir(null)!!, "error_log.txt").appendText(info)
			}
		} catch(e: Throwable) {
			// ignore errors
		}
	}
	
	
	private suspend fun getErrorInfo(
		error: Throwable?,
		description: String,
		diagnostics: List<DiagnosticItem>,
		location: String
	) = """
		[SelfTestMacro ${App.version}] $contextName: $description
		Location: $location
		Android sdk version: ${android.os.Build.VERSION.SDK_INT}
		Model: ${android.os.Build.DEVICE} / ${android.os.Build.PRODUCT}
		Stacktrace:
		${error?.stackTraceToString()}
		
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
