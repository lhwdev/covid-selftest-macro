package com.lhwdev.selfTestMacro.debug

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DebugContext(
	val context: Context,
	val debugEnabled: Boolean,
	val debuggableWithIde: Boolean,
	private val scope: CoroutineScope
) {
	fun onError(
		message: String,
		throwable: Throwable,
		forceShow: Boolean = false
	) {
		scope.launch {
			context.onError(error = throwable, description = message, forceShow = forceShow)
		}
	}
	
	fun onLightError(
		message: String,
		throwable: Throwable? = null,
		shortLog: Boolean = false
	) {
		
	}
	
	
	private suspend fun getErrorInfo(error: Throwable, description: String) = """
		$description
		Android sdk version: ${android.os.Build.VERSION.SDK_INT}
		Model: ${android.os.Build.DEVICE} / ${android.os.Build.PRODUCT}
		Stacktrace:
		${error.stackTraceToString()}
		
		Logcat:
		${getLogcat()}
		
		------------------------------------------------------------------------------------------------
		
	""".trimIndent()
	
	private suspend fun getLogcat(): String = withContext(Dispatchers.IO) {
		val command = arrayOf("logcat", "-d", "-v", "threadtime")
		
		@Suppress("BlockingMethodInNonBlockingContext")
		val process = Runtime.getRuntime().exec(command)
		process.inputStream.reader().use { it.readText() }
	}
}
