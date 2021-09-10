package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.mutableStateListOf



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
	
	fun replaceRoute(route: Route): Boolean = if(currentRoute == routes.last()) {
		replaceLastRoute(route)
		true
	} else {
		false
	}
}
