package com.lhwdev.selfTestMacro

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


/**
 * used for optimization
 */
@Composable
fun EmptyRestartable(content: @Composable () -> Unit) {
	content()
}


@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun changed(value: Any?): Boolean = currentComposer.changed(value)


@Composable
fun <T> lazyState(
	defaultValue: T,
	key: Any? = null,
	allowInit: Boolean = true,
	init: suspend CoroutineScope.() -> T
): State<T> {
	val state = remember { mutableStateOf(defaultValue) }
	val allowInitState by rememberUpdatedState(allowInit)
	LaunchedEffect(key) {
		snapshotFlow { allowInitState }.takeWhile { !it }.collect()
		state.value = init()
	}
	return state
}


suspend inline fun showRouteUnit(
	route: Route,
	crossinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRoute<Unit>(route) {
		content { it(Unit) }
	}
}

suspend fun <T> showRoute(
	route: Route,
	content: @Composable (removeRoute: (T) -> Unit) -> Unit
): T? {
	lateinit var rootContent: @Composable () -> Unit
	
	return suspendCancellableCoroutine { cont ->
		val removeRoute = { result: T? ->
			route.removeRoute(rootContent)
			cont.resume(result)
		}
		
		rootContent = {
			content(removeRoute)
		}
		
		route.add(rootContent)
		cont.invokeOnCancellation { removeRoute(null) }
	}
}
