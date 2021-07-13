@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs


val LocalNavigator = compositionLocalOf<Navigator> { error("not provided") }
val LocalCurrentNavigator = compositionLocalOf<CurrentNavigator> { error("not provided") }

val currentNavigator: CurrentNavigator
	@Composable get() = LocalCurrentNavigator.current


interface Navigator {
	val routes: List<Route>
	
	val size: Int
	
	fun pushRoute(route: Route)
	
	fun popLastRoute()
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
	
	override fun popLastRoute() {
		val route = list.removeLast()
		if(route is RouteObserver) route.onRouteRemoved(this)
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
}

@Composable
fun RouteContent(route: Route) {
	val navigator = LocalNavigator.current
	val currentNav = remember(navigator, route) { CurrentNavigator(navigator, route) }
	CompositionLocalProvider(LocalCurrentNavigator provides currentNav) {
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


fun Route(content: @Composable () -> Unit): Route = object : Route {
	override val content: @Composable () -> Unit = content
}

fun DialogRoute(content: @Composable () -> Unit): Route = object : Route, RouteTransition {
	override val content: @Composable () -> Unit get() = content
	
	@Composable
	override fun OnTransition(fraction: () -> Float, content: @Composable () -> Unit) {
		content()
	}
}

inline fun Navigator.pushRoute(noinline content: @Composable () -> Unit) {
	pushRoute(Route(content))
}

inline fun Navigator.replaceLastRoute(noinline content: @Composable () -> Unit) {
	replaceLastRoute(Route(content))
}

inline fun CurrentNavigator.replaceRoute(noinline content: @Composable () -> Unit) {
	replaceRoute(Route(content))
}


suspend inline fun Navigator.showRouteUnit(
	crossinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRoute<Unit> {
		content { it(Unit) }
	}
}

suspend fun <T> Navigator.showRoute(
	routeFactory: (content: @Composable () -> Unit) -> Route = { Route(it) },
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

fun Navigator.showRouteAsync(
	routeFactory: (content: @Composable () -> Unit) -> Route = { Route(it) },
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
