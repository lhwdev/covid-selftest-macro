@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs


val LocalGlobalNavigator = compositionLocalOf<Navigator> { error("not provided") }

@Suppress("CompositionLocalNaming")
@PublishedApi
internal val sLocalCurrentNavigator = compositionLocalOf<CurrentNavigator> { error("not provided") }

inline val LocalNavigator: CurrentNavigator
	@Composable get() = sLocalCurrentNavigator.current


interface Navigator {
	val routes: List<Route>
	
	val size: Int
	
	fun pushRoute(route: Route)
	
	fun popLastRoute(): Boolean
	fun removeRoute(route: Route): Boolean
	fun clearRoute()
	
	fun replaceLastRoute(route: Route)
}


class NavigatorImpl : Navigator {
	private val list: MutableList<Route> = mutableStateListOf()
	override val routes: List<Route> get() = list
	
	override val size: Int get() = list.size
	
	override fun pushRoute(route: Route) {
		list.add(route)
		if(route is RouteObserver) route.onRouteAdded(this)
	}
	
	override fun popLastRoute(): Boolean {
		val route = list.removeLastOrNull() ?: return false
		if(route is RouteObserver) route.onRouteRemoved(this)
		return true
	}
	
	override fun removeRoute(route: Route): Boolean {
		val index = list.lastIndexOf(route)
		if(index == -1) return false
		
		repeat(list.size - index) {
			popLastRoute()
		}
		
		return true
	}
	
	override fun clearRoute() {
		repeat(list.size) {
			popLastRoute()
		}
	}
	
	override fun replaceLastRoute(route: Route) {
		val last = list.last()
		list[list.lastIndex] = route
		
		if(last is RouteObserver) last.onRouteRemoved(this)
		if(route is RouteObserver) route.onRouteAdded(this)
	}
}

class CurrentNavigator(private val navigator: Navigator, val currentRoute: Route) :
	Navigator by navigator {
	
	val isRoot: Boolean get() = routes.firstOrNull() == currentRoute
	
	fun popRoute(): Boolean {
		return removeRoute(currentRoute)
	}
	
	val onPopRoute: () -> Unit = { popRoute() } // common verbatim for event listener
	
	fun replaceRoute(route: Route): Boolean = if(route == routes.last()) {
		replaceLastRoute(route)
		true
	} else {
		false
	}
}

interface Route {
	val content: @Composable () -> Unit
	val isOpaque: Boolean
}

@Composable
fun RouteContent(route: Route) {
	val navigator = LocalGlobalNavigator.current
	val currentNav = remember(navigator, route) { CurrentNavigator(navigator, route) }
	CompositionLocalProvider(sLocalCurrentNavigator provides currentNav) {
		route.content()
	}
}

interface RouteObserver {
	fun onRouteAdded(navigator: Navigator)
	fun onRouteRemoved(navigator: Navigator)
}

interface RouteTransition {
	@Composable
	fun OnTransition(fraction: () -> Float, content: @Composable () -> Unit)
}

val DefaultRouteTransition: RouteTransition = object : RouteTransition {
	@Composable
	override fun OnTransition(fraction: () -> Float, content: @Composable () -> Unit) {
		Box(Modifier.graphicsLayer { alpha = 1 - abs(1 - fraction()) }) {
			content()
		}
	}
}


fun Route(isOpaque: Boolean = true, content: @Composable () -> Unit): Route = object : Route {
	override val content: @Composable () -> Unit = content
	override val isOpaque: Boolean = isOpaque
}

fun DialogRoute(
	isOpaque: Boolean = false, content: @Composable () -> Unit
): Route = object : Route, RouteTransition {
	override val content: @Composable () -> Unit get() = content
	override val isOpaque: Boolean = isOpaque
	
	@Composable
	override fun OnTransition(fraction: () -> Float, content: @Composable () -> Unit) {
		content()
	}
}

inline fun Navigator.pushRoute(isOpaque: Boolean = true, noinline content: @Composable () -> Unit) {
	pushRoute(Route(isOpaque, content))
}

inline fun Navigator.replaceLastRoute(
	isOpaque: Boolean = true,
	noinline content: @Composable () -> Unit
) {
	replaceLastRoute(Route(isOpaque, content))
}

inline fun CurrentNavigator.replaceRoute(
	isOpaque: Boolean = true,
	noinline content: @Composable () -> Unit
) {
	replaceRoute(Route(isOpaque, content))
}


suspend inline fun Navigator.showRouteUnit(
	isOpaque: Boolean = true,
	crossinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRoute<Unit>(isOpaque) {
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
	noinline content: @Composable (removeRoute: (T) -> Unit) -> Unit
): T = showRoute(routeFactory = { Route(isOpaque, it) }, content = content)

fun Navigator.showRouteAsync(
	routeFactory: (content: @Composable () -> Unit) -> Route,
	content: @Composable (removeRoute: () -> Unit) -> Unit
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
	noinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRouteAsync(routeFactory = { Route(isOpaque, it) }, content = content)
}
