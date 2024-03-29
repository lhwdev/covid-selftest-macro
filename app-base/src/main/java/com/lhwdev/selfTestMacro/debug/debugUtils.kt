package com.lhwdev.selfTestMacro.debug

import android.content.Context
import android.util.Log
import com.lhwdev.selfTestMacro.database.preferenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.Writer
import java.util.WeakHashMap


val Context.isDebugEnabled get() = preferenceState.isDebugEnabled

var debugCheck: Boolean = false


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

var logOutput: Writer? = null

fun log(message: String) {
	Log.d("SelfTestMacro", message)
	
	val output = logOutput
	if(output != null) try {
		output.appendLine(message)
		output.flush()
	} catch(th: Throwable) {
		Log.d("SelfTestMacro", "[Log] error while printing error: $th")
	}
}

fun debugFlow(message: String) {
	if(debugCheck) log(message)
}
