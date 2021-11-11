package com.lhwdev.selfTestMacro.debug

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


val LocalDebugContext: ProvidableCompositionLocal<ComposeDebugContext> =
	compositionLocalOf { error("Not provided") }


@Composable
fun rememberDebugContext(
	flags: DebugContext.DebugFlags
): ComposeDebugContext {
	val androidContext = LocalContext.current
	val uiScope = rememberCoroutineScope()
	val workScope = remember { CoroutineScope(Dispatchers.Default) }
	
	val context = remember {
		ComposeDebugContext(
			context = androidContext,
			flags = flags,
			uiScope = uiScope,
			workScope = workScope
		)
	}
	context.context = androidContext
	return context
}


class ComposeDebugContext(
	override var context: Context,
	flags: DebugFlags,
	val uiScope: CoroutineScope,
	workScope: CoroutineScope
) : DebugContext(flags, workScope) {
	override var contextName: String = ""
}
