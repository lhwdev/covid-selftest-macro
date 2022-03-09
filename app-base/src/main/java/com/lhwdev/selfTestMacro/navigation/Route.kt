@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


data class Route(
	val name: String? = null,
	val isOpaque: Boolean? = null,
	val transition: RouteTransition? = null,
	val onRouteAdded: (() -> Unit)? = null,
	val onRouteRemoved: (() -> Unit)? = null,
	val extras: Map<Key<*>, *>? = null,
	val content: @Composable (() -> Unit)? = null
) {
	companion object {
		val Empty = Route()
		
		fun <T> extra(key: Key<T>, value: T): Route = Route(extras = mapOf(key to value))
	}
	
	open class Key<T>(private val defaultValue: () -> T = { error("defaultValue not provided for $this") }) {
		companion object {
			private val sEmptyCache = Any()
		}
		
		private var defaultCache: Any? = sEmptyCache
		
		fun getDefault(): T = if(defaultCache != sEmptyCache) {
			@Suppress("UNCHECKED_CAST")
			defaultCache as T
		} else {
			val value = defaultValue() // if defaultValue throws exception, fail fast here
			defaultCache = value
			value
		}
	}
	
	
	operator fun <T> get(key: Key<T>): T {
		if(extras != null && key in extras) {
			@Suppress("UNCHECKED_CAST")
			return extras[key] as T
		}
		
		return key.getDefault()
	}
	
	fun <T> withExtra(key: Key<T>, value: T): Route = copy(extras = extras.join(key, value))
	
	
	fun merge(other: Route): Route = Route(
		name = other.name ?: name,
		isOpaque = other.isOpaque ?: isOpaque,
		transition = other.transition ?: transition,
		onRouteAdded = joinFunction(other.onRouteAdded, onRouteAdded),
		onRouteRemoved = joinFunction(other.onRouteRemoved, onRouteRemoved),
		extras = merge(other.extras, extras) { a, b -> a + b },
		content = other.content ?: content,
	)
}


val DialogRouteKey: Route.Key<Boolean> = Route.Key { false }


fun DialogRoute(
	name: String? = null,
	isOpaque: Boolean = false,
	onRouteAdded: (() -> Unit)? = null,
	onRouteRemoved: (() -> Unit)? = null,
	extras: Map<Route.Key<*>, *>? = null,
	content: @Composable () -> Unit
): Route = Route(
	name = name,
	isOpaque = isOpaque,
	transition = NoneTransition(),
	onRouteAdded = onRouteAdded,
	onRouteRemoved = onRouteRemoved,
	extras = extras.join(DialogRouteKey, true),
	content = content
)

fun FullDialogRoute(
	name: String? = null,
	isOpaque: Boolean = true,
	onRouteAdded: (() -> Unit)? = null,
	onRouteRemoved: (() -> Unit)? = null,
	extras: Map<Route.Key<*>, *>? = null,
	content: @Composable () -> Unit
): Route = DialogRoute(
	name = name,
	isOpaque = isOpaque,
	onRouteAdded = onRouteAdded,
	onRouteRemoved = onRouteRemoved,
	extras = extras,
	content = content
)

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


private inline fun <T : Any> merge(a: T?, b: T?, operation: (T, T) -> T): T? = when {
	a == null -> b
	b == null -> a
	else -> operation(a, b)
}

private fun joinFunction(a: (() -> Unit)?, b: (() -> Unit)?) = merge(a, b) { a2, b2 -> { a2(); b2() } }

private fun <K, V> Map<K, V>?.join(key: K, value: V) = if(this == null) {
	mapOf(key to value)
} else {
	this + (key to value)
}
