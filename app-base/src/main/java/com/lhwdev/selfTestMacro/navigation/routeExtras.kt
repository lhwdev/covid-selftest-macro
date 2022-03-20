package com.lhwdev.selfTestMacro.navigation


fun mapOf(vararg extras: Route.Valued<*>): Map<Route.Key<*>, *> = extras.toMap()

fun Array<out Route.Valued<*>>.toMap(): Map<Route.Key<*>, *> = HashMap<Route.Key<*>, Any?>(size).also {
	for((key, value) in this) {
		if(key in it) {
			@Suppress("UNCHECKED_CAST")
			it[key] = (key as Route.Key<Any?>).merge(old = it[key], new = value)
		} else {
			it[key] = value
		}
	}
}


private class RouteListenerKey(name: String) : Route.Key<RouteListener>(name = name, defaultValue = { {} }) {
	override fun merge(old: RouteListener, new: RouteListener): RouteListener = {
		new(it)
		old(it)
	}
}


val OnRouteAdded: Route.Key<RouteListener> = RouteListenerKey("OnRouteAdded")

val OnRouteRemoved: Route.Key<RouteListener> = RouteListenerKey("OnRouteRemoved")


val IsDialogRoute: Route.Key<Boolean> = Route.Key("IsDialogRoute") { false }

val IsOpaque: Route.Key<Boolean> = Route.Key("IsOpaque") { true }

val RouteTransitionBlock: Route.Key<RouteTransition> =
	Route.Key("RouteTransitionBlock") { DefaultTransition(isOpaque = it[IsOpaque]) }
