package com.lhwdev.selfTestMacro.debug

import kotlinx.coroutines.CoroutineScope


class BackgroundDebugContext(
	flags: DebugFlags,
	manager: DebugManager,
	override val contextName: String,
	val uiScope: CoroutineScope
) : DebugContext(flags, manager) {
	override suspend fun onShowErrorInfo(info: ErrorInfo, description: String) {
		
	}
}
