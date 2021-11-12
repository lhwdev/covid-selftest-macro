package com.lhwdev.selfTestMacro.debug

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


val LocalDebugContext: ProvidableCompositionLocal<UiDebugContext> =
	compositionLocalOf { error("Not provided") }


@Composable
fun rememberRootDebugContext(
	contextName: String,
	flags: DebugContext.DebugFlags,
	manager: DebugManager = LocalContext.current.debugManager,
	showErrorInfo: ShowErrorInfo
): UiDebugContext {
	val androidContext = LocalContext.current
	val uiScope = rememberCoroutineScope()
	
	val context = remember {
		UiDebugContext(
			manager = manager,
			context = androidContext,
			contextName = contextName,
			flags = flags,
			uiContext = uiScope.coroutineContext,
			showErrorInfo = showErrorInfo
		)
	}
	context.context = androidContext
	return context
}

@Composable
fun rememberDebugContext(
	contextName: String,
	flags: DebugContext.DebugFlags? = null,
	showErrorInfo: ShowErrorInfo? = null
): UiDebugContext {
	val androidContext = LocalContext.current
	val local = LocalDebugContext.current
	val uiScope = rememberCoroutineScope()
	
	val context = remember {
		UiDebugContext(
			manager = local.manager,
			context = androidContext,
			contextName = contextName,
			flags = flags ?: local.flags,
			uiContext = uiScope.coroutineContext,
			showErrorInfo = showErrorInfo ?: local.showErrorInfo
		)
	}
	context.context = androidContext
	return context
}
