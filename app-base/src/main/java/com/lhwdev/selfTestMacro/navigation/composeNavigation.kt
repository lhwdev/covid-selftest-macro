package com.lhwdev.selfTestMacro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.lhwdev.selfTestMacro.debug.LocalDebugContext
import com.lhwdev.selfTestMacro.debug.rememberDebugContext
import com.lhwdev.selfTestMacro.ui.LocalSnackbarHost


val LocalGlobalNavigator = compositionLocalOf<Navigator> { error("not provided") }

@Suppress("CompositionLocalNaming")
@PublishedApi
internal val sLocalCurrentNavigator = compositionLocalOf<CurrentNavigator> { error("not provided") }

inline val LocalNavigator: CurrentNavigator
	@Composable get() = sLocalCurrentNavigator.current


@Composable
fun RouteContent(route: RouteInstance) {
	val navigator = LocalGlobalNavigator.current
	val currentNav = remember(navigator, route) { CurrentNavigator(navigator, route) }
	
	Surface(color = if(route.route.isOpaque) Color.Transparent else Color.White) {
		val snackbarHostState = remember { SnackbarHostState() }
		val debugContext = rememberDebugContext(contextName = route.route.name ?: "(ComposeRoute)")
		
		CompositionLocalProvider(
			sLocalCurrentNavigator provides currentNav,
			LocalSnackbarHost provides snackbarHostState,
			LocalDebugContext provides debugContext
		) {
			Box {
				route.route.content()
				SnackbarHost(snackbarHostState)
			}
		}
	}
}
