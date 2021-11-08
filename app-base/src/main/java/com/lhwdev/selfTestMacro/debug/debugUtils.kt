package com.lhwdev.selfTestMacro.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.SnackbarHostState
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.packages.api_base.BuildConfig
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext


val Context.isDebugEnabled get() = BuildConfig.DEBUG || preferenceState.isDebugEnabled


// suspend fun onError(th: Throwable, message: String) {
// 	Log.e("SelfTestMacro", message, th)
//	
// 	// in dev mode, usually ran with IDE; can check things with Logcat
// 	if(App.flavor != "dev") writeErrorLog(info)
// }

// suspend fun onError(th: Throwable, message: String) {
// 	Log.e("SelfTestMacro", message, th)
// }


fun selfLog(message: String) {
	Log.d("SelfTestMacro", message)
}

suspend fun Context.onError(
	snackbarHostState: SnackbarHostState,
	message: String,
	throwable: Throwable
) {
	// this is for debugging; intentional
	CoroutineScope(coroutineContext).launch { snackbarHostState.showSnackbar("오류: $message", "확인") }
	onError(throwable, message)
}


suspend fun Context.onError(error: Throwable, description: String = "???", forceShow: Boolean = false) {
	Log.e("ERROR", description, error)
	val info = getErrorInfo(error, description)
	if(forceShow || isDebugEnabled) withContext(Dispatchers.Main) {
		try {
			showErrorInfo(info)
		} catch(th: Throwable) {
		}
	}
	
	// in dev mode, usually ran with IDE; can check things with Logcat
	if(App.flavor != "dev") writeErrorLog(info)
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


private suspend fun Context.showErrorInfo(info: String): Unit = withContext(Dispatchers.Main) {
	AlertDialog.Builder(this@showErrorInfo).apply {
		setTitle("오류 발생")
		setMessage("* 복사된 오류정보는 기기의 정보 등 민감한 정보를 포함할 수 있어요.\n$info")
		
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

