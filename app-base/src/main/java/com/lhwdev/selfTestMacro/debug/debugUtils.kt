package com.lhwdev.selfTestMacro.debug

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debuggingWithIde
import com.lhwdev.selfTestMacro.packages.api_base.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.WeakHashMap


val Context.isDebugEnabled get() = BuildConfig.DEBUG || preferenceState.isDebugEnabled


private val debugManagerMap = WeakHashMap<Context, DebugManager>()

val Context.debugManager: DebugManager
	get() = debugManagerMap.getOrPut(applicationContext) {
		DefaultDebugManager(androidContext = applicationContext, workScope = CoroutineScope(Dispatchers.Default))
	}


private val debugContextMap = WeakHashMap<Context, UiDebugContext>()

val Context.debugContext: UiDebugContext
	get() = debugContextMap.getOrPut(this) {
		UiDebugContext(
			manager = debugManager,
			context = this,
			flags = DebugContext.DebugFlags(
				enabled = isDebugEnabled,
				debuggingWithIde = App.debuggingWithIde
			),
			uiScope = (this as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main.immediate),
			showErrorInfo = UiDebugContext::showErrorDialog
		)
	}

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

