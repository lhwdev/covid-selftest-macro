package com.lhwdev.selfTestMacro.debug

import android.content.Context
import android.util.Log
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.packages.api_base.BuildConfig


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

