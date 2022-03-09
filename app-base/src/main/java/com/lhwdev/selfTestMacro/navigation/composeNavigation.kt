package com.lhwdev.selfTestMacro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.insets.navigationBarsWithImePadding
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
fun RouteContent(navigator: CurrentNavigator) {
	val route = navigator.currentRoute
	Surface(color = if(route.isOpaque) Color.Transparent else Color.White) {
		val snackbarHostState = remember { SnackbarHostState() }
		val debugContext = rememberDebugContext(contextName = route.name ?: "(ComposeRoute)")
		
		CompositionLocalProvider(
			sLocalCurrentNavigator provides navigator,
			LocalSnackbarHost provides snackbarHostState,
			LocalDebugContext provides debugContext
		) {
			Box {
				route.content()
				SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsWithImePadding())
			}
		}
	}
}
