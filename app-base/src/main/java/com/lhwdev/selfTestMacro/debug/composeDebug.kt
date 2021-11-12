package com.lhwdev.selfTestMacro.debug

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


val LocalDebugContext: ProvidableCompositionLocal<UiDebugContext> =
	compositionLocalOf { error("Not provided") }


@Composable
fun rememberRootDebugContext(
	flags: DebugContext.DebugFlags,
	manager: DebugManager = LocalContext.current.debugManager,
	showErrorInfo: ShowErrorInfo
): UiDebugContext {
	val androidContext = LocalContext.current
	val uiScope = rememberCoroutineScope()
	val workScope = remember { CoroutineScope(Dispatchers.Default) }
	
	val context = remember {
		UiDebugContext(
			context = androidContext,
			flags = flags,
			uiContext = uiScope.coroutineContext,
			manager = manager,
			showErrorInfo = showErrorInfo
		)
	}
	context.context = androidContext
	return context
}

@Composable
fun rememberDebugContext(
	flags: DebugContext.DebugFlags? = null,
	showErrorInfo: ShowErrorInfo? = null
): UiDebugContext {
	val androidContext = LocalContext.current
	val local = LocalDebugContext.current
	val uiScope = rememberCoroutineScope()
	
	val context = remember {
		UiDebugContext(
			context = androidContext,
			flags = flags ?: local.flags,
			uiContext = uiScope.coroutineContext,
			manager = local.manager,
			showErrorInfo = showErrorInfo ?: local.showErrorInfo
		)
	}
	context.context = androidContext
	return context
}
