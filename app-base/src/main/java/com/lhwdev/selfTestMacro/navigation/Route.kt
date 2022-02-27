@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


interface Route {
	val content: @Composable () -> Unit
	val isOpaque: Boolean
	
	val name: String? get() = null
}

interface DialogRoute : Route


fun Route.copy( // TODO: no support for RouteObserver
	name: String? = this.name,
	isOpaque: Boolean = this.isOpaque,
	transition: RouteTransition = this as? RouteTransition ?: DefaultTransition(isOpaque),
	content: @Composable () -> Unit = this.content
): Route = Route(name, isOpaque, transition, content)

interface RouteObserver {
	fun onRouteAdded(navigator: Navigator) {}
	fun onRouteRemoved(navigator: Navigator) {}
}


fun Route(
	name: String? = null,
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	content: @Composable () -> Unit
): Route = object : Route, RouteTransition by transition {
	override val name: String? = name
	override val content: @Composable () -> Unit = content
	override val isOpaque: Boolean = isOpaque
	override fun toString(): String = "Route $name(isOpaque=$isOpaque, transition=$transition, $content)"
}


@PublishedApi
internal abstract class DialogRouteBase(
	override val name: String?,
	override val isOpaque: Boolean,
	override val content: @Composable () -> Unit
) : DialogRoute, RouteTransition by NoneTransition(), RouteObserver {
	override fun toString(): String = "Route $name(isOpaque=$isOpaque, $content)"
}

inline fun DialogRoute(
	name: String? = null,
	isOpaque: Boolean = false,
	crossinline onRouteRemoved: () -> Unit,
	noinline content: @Composable () -> Unit
): DialogRoute = object : DialogRouteBase(name = name, isOpaque = isOpaque, content = content) {
	override fun onRouteRemoved(navigator: Navigator) {
		onRouteRemoved()
	}
}

inline fun FullDialogRoute(
	name: String? = null,
	noinline content: @Composable () -> Unit,
	crossinline onRouteRemoved: () -> Unit
): DialogRoute = DialogRoute(name = name, content = content, onRouteRemoved = onRouteRemoved)

inline fun Navigator.pushRoute(
	name: String? = null,
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable () -> Unit
) {
	pushRoute(Route(name, isOpaque, transition, content))
}

inline fun CurrentNavigator.replaceRoute(
	name: String? = null,
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable () -> Unit
): Boolean {
	return replaceRoute(Route(name, isOpaque, transition, content))
}


suspend inline fun Navigator.showRouteUnit(
	name: String? = null,
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	crossinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRoute<Unit>(name, isOpaque, transition) {
		content { it(Unit) }
	}
}


typealias RouteFactory<T> = (removeRoute: (T) -> Unit) -> Route
typealias ContentRouteFactory<T> = (content: @Composable () -> Unit, removeRoute: (T) -> Unit) -> Route


suspend fun <T> Navigator.showRouteFactory(routeFactory: RouteFactory<T>): T {
	lateinit var route: Route
	
	return suspendCancellableCoroutine { cont ->
		val removeRoute =
			{ result: T -> // removeRoute should be able to be called several times, ignoring following calls.
				val removed = removeRoute(route)
				if(removed) {
					cont.resume(result)
				}
			}
		
		route = routeFactory(removeRoute)
		
		pushRoute(route)
		cont.invokeOnCancellation { removeRoute(route) }
	}
}

suspend inline fun <T> Navigator.showRoute(
	name: String? = null,
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable (removeRoute: (T) -> Unit) -> Unit
): T = showRouteFactory { Route(name, isOpaque, transition) { content(it) } }

inline fun Navigator.showRouteFactoryAsync(routeFactory: RouteFactory<Nothing?>) {
	lateinit var route: Route
	
	val removeRoute = { _: Nothing? ->
		removeRoute(route)
	}
	
	route = routeFactory(removeRoute)
	
	pushRoute(route)
}

inline fun Navigator.showRouteAsync(route: Route) {
	showRouteFactoryAsync { route }
}

inline fun Navigator.showRouteAsync(
	name: String? = null,
	isOpaque: Boolean = true,
	transition: RouteTransition = DefaultTransition(isOpaque),
	noinline content: @Composable (removeRoute: () -> Unit) -> Unit
) {
	showRouteFactoryAsync { Route(name, isOpaque, transition) { content { it(null) } } }
}
