@file:Suppress("NOTHING_TO_INLINE")

package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


typealias RouteListener = (Navigator) -> Unit


data class Route(
	val name: String? = null,
	val extras: Map<Key<*>, *>? = null,
	val content: @Composable (() -> Unit)? = null
) {
	constructor(
		vararg extras: Valued<*>,
		name: String? = null,
		content: @Composable (() -> Unit)? = null
	) : this(name, extras.toMap(), content)
	
	companion object {
		val Empty = Route()
	}
	
	open class Key<T>(
		val name: String? = null,
		private val defaultValue: (from: Route) -> T = { error("defaultValue not provided for $this") }
	) {
		fun getDefault(from: Route): T = defaultValue(from)
		
		infix fun to(value: T): Valued<T> = Valued(this, value)
		
		open fun merge(old: T, new: T): T = new
		
		override fun toString(): String = name ?: super.toString()
	}
	
	data class Valued<T>(val key: Key<T>, val value: T)
	
	
	operator fun <T> get(key: Key<T>): T {
		if(extras != null && key in extras) {
			@Suppress("UNCHECKED_CAST")
			return extras[key] as T
		}
		
		return key.getDefault(from = this)
	}
	
	fun withExtras(vararg extras: Valued<*>): Route = copy(extras = this.extras.mergeExtras(extras.toMap()))
	
	
	fun merge(other: Route): Route = Route(
		name = other.name ?: name,
		extras = extras.mergeExtras(other.extras),
		content = other.content ?: content,
	)
	
	override fun toString(): String = "Route(name=$name, ${extras?.toString() ?: "extras={}"})"
}

fun Map<Route.Key<*>, *>.mergeExtras(other: Map<Route.Key<*>, *>): Map<Route.Key<*>, *> {
	// a over b
	val map = HashMap(this)
	for((key, value) in other) {
		if(key in map) {
			val last = map[key]
			@Suppress("UNCHECKED_CAST")
			key as Route.Key<Any?>
			map[key] = key.merge(old = last, new = value)
		} else {
			map[key] = value
		}
	}
	return map
}

@JvmName("mergeExtrasNullable")
fun Map<Route.Key<*>, *>?.mergeExtras(other: Map<Route.Key<*>, *>?): Map<Route.Key<*>, *>? =
	merge(this, other) { a, b -> a.mergeExtras(b) }


fun DialogRoute(
	vararg extras: Route.Valued<*>,
	name: String? = null,
	content: @Composable () -> Unit
): Route = Route(
	extras = mapOf(
		IsDialogRoute to true,
		IsOpaque to false,
		RouteTransitionBlock to NoneTransition()
	).mergeExtras(extras.toMap()),
	name = name,
	content = content
)

fun FullDialogRoute(
	vararg extras: Route.Valued<*>,
	name: String? = null,
	content: @Composable () -> Unit
): Route = DialogRoute(
	extras = arrayOf(IsOpaque to true, *extras),
	name = name,
	content = content
)

inline fun Navigator.pushRoute(
	route: Route = Route.Empty,
	noinline content: @Composable () -> Unit
) {
	pushRoute(route.copy(content = content))
}


typealias RouteFactory<T> = (removeRoute: (T) -> Unit) -> Route

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

inline fun Navigator.showRouteFactoryAsync(routeFactory: RouteFactory<Nothing?>) {
	lateinit var route: Route
	
	val removeRoute = { _: Nothing? ->
		removeRoute(route)
	}
	
	route = routeFactory(removeRoute)
	
	pushRoute(route)
}


private inline fun <T : Any> merge(a: T?, b: T?, operation: (T, T) -> T): T? = when {
	a == null -> b
	b == null -> a
	else -> operation(a, b)
}
