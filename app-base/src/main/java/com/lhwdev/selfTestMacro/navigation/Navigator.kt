package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope



interface Navigator {
	val routes: List<RouteInstance>
	
	val size: Int
	
	fun pushRoute(route: RouteInstance)
	
	fun popLastRoute(): Boolean
	fun removeRoute(route: Route): Boolean
	fun removeRoute(route: RouteInstance): Boolean
	fun removeRoutes(startIndex: Int): Boolean
	fun clearRoute()
}

fun Navigator.pushRoute(route: Route) {
	pushRoute(RouteInstance(route))
}



class RouteInstance(val route: Route) {
	override fun toString(): String = route.toString()
}


@Stable
var sDebugNavigation: Boolean by mutableStateOf(false)


class NavigatorImpl : Navigator {
	private val list: MutableList<RouteInstance> = mutableStateListOf()
	override val routes: List<RouteInstance> get() = list
	
	override val size: Int get() = list.size
	
	override fun pushRoute(route: RouteInstance) {
		list.add(route)
		if(sDebugNavigation) println("pushRoute $route")
		route.route[OnRouteAdded](this)
	}
	
	override fun popLastRoute(): Boolean {
		val route = list.removeLastOrNull() ?: return false
		if(sDebugNavigation) println("popLastRoute $route")
		route.route[OnRouteRemoved](this)
		return true
	}
	
	override fun removeRoute(route: Route): Boolean =
		removeRoutes(list.indexOfLast { it.route === route })
	
	override fun removeRoute(route: RouteInstance): Boolean =
		removeRoutes(list.lastIndexOf(route))
	
	override fun removeRoutes(startIndex: Int): Boolean {
		if(startIndex == -1) return false
		
		repeat(list.size - startIndex) {
			popLastRoute()
		}
		
		return true
	}
	
	override fun clearRoute() {
		repeat(list.size) {
			popLastRoute()
		}
	}
}

class CurrentNavigator(
	val rootNavigator: Navigator,
	val currentRouteInstance: RouteInstance,
	val coroutineScope: CoroutineScope
) : Navigator by rootNavigator {
	val currentRoute: Route get() = currentRouteInstance.route
	
	var parent: CurrentNavigator? = null
		private set
	
	val parentOrSelf: CurrentNavigator get() = parent ?: this
	
	var isVisible: Boolean by mutableStateOf(true)
		private set
	
	val isRoot: Boolean get() = routes.firstOrNull() == currentRouteInstance
	val isTop: Boolean get() = routes.last() == currentRouteInstance
	
	val index: Int get() = routes.indexOf(currentRouteInstance)
	
	val parents: Iterable<CurrentNavigator>
		get() = object : Iterable<CurrentNavigator> {
			override fun iterator(): Iterator<CurrentNavigator> = object : Iterator<CurrentNavigator> {
				private var current: CurrentNavigator = this@CurrentNavigator
				override fun hasNext(): Boolean = current.parent != null
				override fun next(): CurrentNavigator = current.parent!!.also { current = it }
			}
		}
	
	internal fun updateState(parent: CurrentNavigator?, isVisible: Boolean) {
		this.parent = parent
		this.isVisible = isVisible
	}
	
	fun popRoute(): Boolean {
		return removeRoute(currentRouteInstance)
	}
	
	fun replaceRoute(route: Route): Boolean {
		val result = removeRoute(currentRouteInstance)
		if(!result) {
			println("[Navigator] couldn't replace route $route: not exist in routes")
			return false
		}
		
		pushRoute(route)
		return true
	}
	
	fun replaceChildren(route: Route) {
		removeChildren()
		pushRoute(route)
	}
	
	fun removeChildren() {
		if(!isTop) removeRoutes(startIndex = index + 1)
	}
}
