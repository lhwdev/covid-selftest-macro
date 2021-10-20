package com.lhwdev.selfTestMacro.debug

import android.content.Context
import kotlinx.coroutines.CoroutineScope


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
		context.onError()
	}
	
	fun onLightError(
		message: String,
		throwable: Throwable? = null,
		shortLog: Boolean = false
	) {
		
	}
}
