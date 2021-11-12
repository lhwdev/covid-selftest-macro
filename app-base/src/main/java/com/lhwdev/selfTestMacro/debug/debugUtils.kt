package com.lhwdev.selfTestMacro.debug

import android.content.Context
import android.util.Log
import com.lhwdev.selfTestMacro.database.preferenceState
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


// As we don't write UI with Android framework, this is not needed

// private val debugContextMap = WeakHashMap<Context, UiDebugContext>()
//
// val Context.debugContext: UiDebugContext
// 	get() = debugContextMap.getOrPut(this) {
// 		UiDebugContext(
// 			manager = debugManager,
// 			context = this,
// 			flags = DebugContext.DebugFlags(
// 				enabled = isDebugEnabled,
// 				debuggingWithIde = App.debuggingWithIde
// 			),
// 			uiScope = (this as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main.immediate),
// 			showErrorInfo = UiDebugContext::showErrorDialog
// 		)
// 	}

fun selfLog(message: String) {
	Log.d("SelfTestMacro", message)
}
