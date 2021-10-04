package com.lhwdev.selfTestMacro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

val Context.isDebugEnabled get() = BuildConfig.DEBUG || preferenceState.isDebugEnabled

private var logOutput: File? = null

internal const val sSelfLog = "log/self_log.txt"
internal const val sErrorLog = "log/error_log.txt"


fun Context.shareErrorLog(file: String) {
	val uri = FileProvider.getUriForFile(
		this,
		"com.lhwdev.selfTestMacro.file_provider",
		File(getExternalFilesDir(null)!!, file)
	)
	
	val intent = Intent().apply {
		action = Intent.ACTION_SEND
		putExtra(Intent.EXTRA_STREAM, uri)
		type = "text/plain"
		addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
	}
	val chooser = Intent.createChooser(intent, "로그 공유")
	startActivity(chooser)
}

fun Context.selfLog(description: String, error: Throwable? = null, force: Boolean = false) {
	Log.i("SelfTestMacro", description, error)
	if(!force && !isDebugEnabled) return
	
	val log = logOutput ?: run {
		val log = File(getExternalFilesDir(null)!!, sSelfLog)
		logOutput = log
		log
	}
	
	val text = buildString {
		append(Date().toString())
		append(": ")
		append(description)
		if(error != null) {
			append('\n')
			append(error.stackTraceToString())
		}
		append('\n')
	}
	try {
		log.appendText(text)
	} catch(th: Throwable) { Log.e("SelfTestMacro", "Error while logging '$description'", th) }
}

suspend fun Context.onErrorToast(error: Throwable, description: String = "???") {
	showToastSuspendAsync("오류가 발생했습니다. ($description)")
	onError(error, description)
}

suspend inline fun <R : Any> Context.catchErrorThanToast(description: String = "???", block: () -> R): R? =
	try {
		block()
	} catch(e: Throwable) {
		onErrorToast(e, description)
		null
	}


suspend fun Context.onError(error: Throwable, description: String = "???", forceShow: Boolean = false) {
	Log.e("ERROR", description, error)
	val info = getErrorInfo(error, description)
	if(forceShow || isDebugEnabled) withContext(Dispatchers.Main) {
		showErrorInfo(info)
	}
	writeErrorLog(info)
}

suspend fun Context.writeErrorLog(info: String) {
	try {
		withContext(Dispatchers.IO) {
			File(getExternalFilesDir(null)!!, sErrorLog).appendText(info)
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
	@Suppress("BlockingMethodInNonBlockingContext")
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

