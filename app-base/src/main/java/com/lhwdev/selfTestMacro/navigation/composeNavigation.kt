package com.lhwdev.selfTestMacro.navigation

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color


val LocalGlobalNavigator = compositionLocalOf<Navigator> { error("not provided") }

@Suppress("CompositionLocalNaming")
@PublishedApi
internal val sLocalCurrentNavigator = compositionLocalOf<CurrentNavigator> { error("not provided") }

inline val LocalNavigator: CurrentNavigator
	@Composable get() = sLocalCurrentNavigator.current


@Composable
fun RouteContent(route: Route) {
	val navigator = LocalGlobalNavigator.current
	val currentNav = remember(navigator, route) { CurrentNavigator(navigator, route) }
	
	CompositionLocalProvider(sLocalCurrentNavigator provides currentNav) {
		Surface(color = if(route.isOpaque) Color.Transparent else Color.White) {
			route.content()
		}
	}
}
