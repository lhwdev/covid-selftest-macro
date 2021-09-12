@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


interface Route {
	val content: @Composable () -> Unit
	val isOpaque: Boolean
}


interface RouteObserver {
	fun onRouteAdded(navigator: Navigator)
	fun onRouteRemoved(navigator: Navigator)
}


fun Route(
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	content: @Composable () -> Unit
): Route = object : Route, RouteTransition by transition {
	override val content: @Composable () -> Unit = content
	override val isOpaque: Boolean = isOpaque
}


fun DialogRoute(
	isOpaque: Boolean = false,
	content: @Composable () -> Unit
): Route = object : Route, RouteTransition by NoneTransition() {
	override val content: @Composable () -> Unit get() = content
	override val isOpaque: Boolean = isOpaque
}

fun FullDialogRoute(
	content: @Composable () -> Unit
): Route = object : Route, RouteTransition by CustomDialogRouteTransition() {
	override val content: @Composable () -> Unit = content
	override val isOpaque: Boolean get() = true
}

inline fun Navigator.pushRoute(
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable () -> Unit
) {
	pushRoute(Route(isOpaque, transition, content))
}

inline fun CurrentNavigator.replaceRoute(
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable () -> Unit
): Boolean {
	return replaceRoute(Route(isOpaque, transition, content))
}


suspend inline fun Navigator.showRouteUnit(
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	crossinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRoute<Unit>(isOpaque, transition) {
		content { it(Unit) }
	}
}


suspend fun <T> Navigator.showRoute(
	routeFactory: (content: @Composable () -> Unit) -> Route,
	content: @Composable (removeRoute: (T) -> Unit) -> Unit
): T {
	lateinit var route: Route
	
	return suspendCancellableCoroutine { cont ->
		val removeRoute = { result: T ->
			removeRoute(route)
			cont.resume(result)
		}
		
		route = routeFactory {
			content(removeRoute)
		}
		
		pushRoute(route)
		cont.invokeOnCancellation { removeRoute(route) }
	}
}

suspend inline fun <T> Navigator.showRoute(
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable (removeRoute: (T) -> Unit) -> Unit
): T = showRoute(routeFactory = { Route(isOpaque, transition, it) }, content = content)

inline fun Navigator.showRouteAsync(
	routeFactory: (content: @Composable () -> Unit) -> Route,
	crossinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	lateinit var route: Route
	
	val removeRoute = {
		removeRoute(route)
	}
	
	route = routeFactory {
		content(removeRoute)
	}
	
	pushRoute(route)
}

inline fun Navigator.showRouteAsync(
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRouteAsync(routeFactory = { Route(isOpaque, transition, it) }, content = content)
}
