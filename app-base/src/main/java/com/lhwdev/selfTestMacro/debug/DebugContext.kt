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


abstract class DebugContext(
	val flags: DebugFlags,
	private val workScope: CoroutineScope
) {
	data class DebugFlags(
		val enabled: Boolean,
		val debuggingWithIde: Boolean
	)
	
	class ErrorInfo(
		val message: String,
		val detailedMessage: String,
		val throwable: Throwable?,
		val diagnostics: List<DiagnosticItem>
	)
	
	
	companion object {
		val ShowErrorDialog = { debugContext: DebugContext, errorInfo: ErrorInfo ->
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
	
	abstract val context: Context
	
	
	
	fun onError(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false
	) {
		workScope.launch {
			onErrorSuspend(message, throwable, diagnostics, forceShow)
		}
	}
	
	suspend fun onErrorSuspend(
		message: String,
		throwable: Throwable?,
		diagnostics: List<DiagnosticItem> = emptyList(),
		forceShow: Boolean = false
	) {
		Log.e("ERROR", message, throwable)
		val info = getErrorInfo(throwable, message, diagnostics)
		if(forceShow || flags.enabled) {
			showErrorInfo(ErrorInfo(message, info, throwable, diagnostics))
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
		shortLog: Boolean = false
	) {
		workScope.launch {
			onLightErrorSuspend(message, throwable, diagnostics, shortLog)
		}
	}
	
	suspend fun onLightErrorSuspend(
		message: String,
		throwable: Throwable? = null,
		diagnostics: List<DiagnosticItem> = emptyList(),
		shortLog: Boolean = false
	) {
		Log.e("ERROR", message, throwable)
		
		// in dev mode, usually ran with IDE; can check things with Logcat
		if(!flags.debuggingWithIde) {
			val info = if(shortLog) {
				"[SelfTestMacro ${App.version}]"
			} else {
				getErrorInfo(throwable, message, diagnostics)
			}
			writeErrorLog(info)
		}
	}
	
	
	private suspend fun showErrorInfo(info: ErrorInfo): Unit = withContext(Dispatchers.Main) {
		try {
			
		} catch(th: Throwable) {
			onLightError("showErrorInfo was failed", throwable = th)
		}
	}
	
	
	private suspend fun writeErrorLog(info: String) {
		try {
			withContext(Dispatchers.IO) {
				File(context.getExternalFilesDir(null)!!, "error_log.txt").appendText(info)
			}
		} catch(e: Throwable) {
			// ignore errors
		}
	}
	
	
	private suspend fun getErrorInfo(error: Throwable?, description: String, diagnostics: List<DiagnosticItem>) = """
		[SelfTestMacro ${App.version}] $contextName: $description
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
