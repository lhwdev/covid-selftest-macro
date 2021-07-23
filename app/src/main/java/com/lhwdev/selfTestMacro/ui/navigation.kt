@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


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
	fun Transition(
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	)
}

@Composable
fun OnTransitionEndObserver(transition: Transition<*>, onAnimationEnd: () -> Unit) {
	LaunchedEffect(transition) {
		snapshotFlow { transition.currentState }
			.collect {
				onAnimationEnd()
			}
	}
}

// note: this does not support snapping externally
@Composable
fun updateRouteTransition(visibleState: VisibilityAnimationState): Transition<Boolean> {
	val transitionState = remember {
		MutableTransitionState(initialState = visibleState == VisibilityAnimationState.visible)
	}
	transitionState.targetState = visibleState.targetState
	
	return updateTransition(transitionState, label = "updateRouteTransition")
}

val DefaultOpaqueRouteTransition: RouteTransition = object : RouteTransition {
	@Composable
	override fun Transition(
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		println(visibleState)
		Text("HO $visibleState")
		val transition = updateRouteTransition(visibleState)
		
		OnTransitionEndObserver(transition, onAnimationEnd)
		
		val scrimTransparency = transition.animateFloat(label = "ScrimTransparency",
			transitionSpec = { tween(30000) }) {
			if(it) 1f else 0f
		}
		
		Box {
			Box(
				Modifier
					.matchParentSize()
					.graphicsLayer { alpha = scrimTransparency.value }
					.background(Color.Black.copy(alpha = 0.5f))
			)
			transition.AnimatedVisibility(
				visible = { it },
				enter = slideInHorizontally(initialOffsetX = { it }, tween(30000)),
				exit = slideOutHorizontally(targetOffsetX = { it }, tween(30000)),
			) { content() }
		}
	}
}

val DefaultTransparentRouteTransition: RouteTransition = object : RouteTransition {
	@Composable
	override fun Transition(
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		val transition = updateRouteTransition(visibleState)
		
		OnTransitionEndObserver(transition, onAnimationEnd)
		
		transition.AnimatedVisibility(
			visible = { it },
			enter = fadeIn(),
			exit = fadeOut()
		) { content() }
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
	override fun Transition(
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		content()
		if(changed(visibleState)) {
			onAnimationEnd()
		}
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
