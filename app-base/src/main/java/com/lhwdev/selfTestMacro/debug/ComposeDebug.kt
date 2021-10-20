package com.lhwdev.selfTestMacro.debug

import android.content.Context


class DebugContext(
	val context: Context,
	val debugEnabled: Boolean,
	val debuggableWithIde: Boolean
) {
	fun onError(
		message: String,
		throwable: Throwable,
		forceShow: Boolean = false
	) {
		
	}
}
