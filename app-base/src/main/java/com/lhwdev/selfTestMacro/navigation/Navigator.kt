package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf



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
var sDebugNavigation: Boolean = false


class NavigatorImpl : Navigator {
	private val list: MutableList<RouteInstance> = mutableStateListOf()
	override val routes: List<RouteInstance> get() = list
	
	override val size: Int get() = list.size
	
	override fun pushRoute(route: RouteInstance) {
		list.add(route)
		if(sDebugNavigation) println("pushRoute $route")
		if(route.route is RouteObserver) route.route.onRouteAdded(this)
	}
	
	override fun popLastRoute(): Boolean {
		val route = list.removeLastOrNull() ?: return false
		if(sDebugNavigation) println("popLastRoute $route")
		if(route.route is RouteObserver) route.route.onRouteRemoved(this)
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

class CurrentNavigator(val rootNavigator: Navigator, val currentRoute: RouteInstance) : Navigator by rootNavigator {
	val isRoot: Boolean get() = routes.firstOrNull() == currentRoute
	val isTop: Boolean get() = routes.last() == currentRoute
	
	val index: Int get() = routes.indexOf(currentRoute)
	
	val parent: CurrentNavigator?
		get() = routes.getOrNull(index - 1)?.let {
			CurrentNavigator(rootNavigator = rootNavigator, currentRoute = it)
		}
	
	val opaqueParent: CurrentNavigator?
		get() {
			val route = routes.subList(0, index).lastOrNull { it.route.isOpaque } ?: return null
			
			return CurrentNavigator(
				rootNavigator = rootNavigator,
				currentRoute = route
			)
		}
	
	
	fun popRoute(): Boolean {
		return removeRoute(currentRoute)
	}
	
	fun replaceRoute(route: Route): Boolean {
		val result = removeRoute(currentRoute)
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
