package com.lhwdev.selfTestMacro.debug

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext


class BackgroundDebugContext(
	flags: DebugFlags,
	manager: DebugManager,
	override val contextName: String,
	val uiContext: CoroutineContext = Dispatchers.Main
) : DebugContext(flags, manager) {
	override suspend fun onShowErrorInfo(info: ErrorInfo, description: String) {
		
	}
}
