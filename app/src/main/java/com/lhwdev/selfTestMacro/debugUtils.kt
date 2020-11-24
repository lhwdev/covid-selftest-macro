package com.lhwdev.selfTestMacro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


suspend fun Context.onError(error: Throwable, description: String = "error") {
	val info = getErrorInfo(error, description)
	if(getExternalFilesDir(null)?.let { File(it, "debug.txt").exists() } == true) withContext(
		Dispatchers.Main
	) {
		showErrorInfo(info)
	}
	writeErrorLog(info)
}

suspend fun Context.writeErrorLog(info: String) {
  try {
  	withContext(Dispatchers.IO) {
  		File(getExternalFilesDir(null)!!, "error_log.txt").appendText(info)
  	}
	} catch(e: Throwable) {
	  // ignore errors
	}
}

suspend fun getErrorInfo(error: Throwable, description: String) = """
	$description
	Android sdk version: ${Build.VERSION.SDK_INT}
	Model: ${Build.DEVICE} / ${Build.PRODUCT}
	Stacktrace:
	${error.stackTraceToString()}
	
	Logcat:
	${getLogcat()}
""".trimIndent()

private fun Context.showErrorInfo(info: String) {
	AlertDialog.Builder(this).apply {
		setTitle("오류 발생")
		setMessage("* 복사된 오류정보는 기기의 정보 등 민감한 정보를 포함할 수 있습니다.\n$info")
		
		setPositiveButton("오류정보 복사") { _, _ ->
			CoroutineScope(Dispatchers.Main).launch {
				getSystemService<ClipboardManager>()!!
					.setPrimaryClip(ClipData.newPlainText("오류정보", info))
				showToastSuspendAsync("복사 완료")
			}
		}
		setNegativeButton("취소", null)
	}.show()
}


suspend fun getLogcat(): String = withContext(Dispatchers.IO) {
	val command = arrayOf("logcat", "-d", "-v", "threadtime")
	val process = Runtime.getRuntime().exec(command)
	process.inputStream.reader().use { it.readText() }
}

//	//Code here
//	val log: StringBuilder
//		get() {
//			val builder = StringBuilder()
//			try {
//				val command = arrayOf("logcat", "-d", "-v", "threadtime")
//				val process = Runtime.getRuntime().exec(command)
//				val bufferedReader = process.inputStream.bufferedReader()
//
//
//				var line: String
//				while(bufferedReader.readLine().also { line = it } != null) {
//					if(line.contains(processId)) {
//						builder.append(line)
//						//Code here
//					}
//				}
//			} catch(ex: IOException) {
//				Log.e(TAG, "getLog failed", ex)
//			}
//			return builder
//		}

