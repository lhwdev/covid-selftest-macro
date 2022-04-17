package com.lhwdev.selfTestMacro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import com.lhwdev.selfTestMacro.debug.LocalDebugContext
import com.lhwdev.selfTestMacro.debug.rememberDebugContext
import com.lhwdev.selfTestMacro.ui.LocalSnackbarHost
import com.lhwdev.selfTestMacro.ui.assertConstant
import com.lhwdev.selfTestMacro.utils.LocalLifecycleState


val LocalGlobalNavigator = compositionLocalOf<Navigator> { error("not provided") }

@Suppress("CompositionLocalNaming")
@PublishedApi
internal val sLocalCurrentNavigator = compositionLocalOf<CurrentNavigator> { error("not provided") }

inline val LocalNavigator: CurrentNavigator
	@Composable get() = sLocalCurrentNavigator.current


@Composable
fun RouteContent(navigator: CurrentNavigator) {
	val route = navigator.currentRoute
	
	Surface(color = if(route[IsOpaque]) Color.Transparent else Color.White) {
		val previousLifecycle = LocalLifecycleState.current
		val lifecycle = remember { // every Route which is inactive should be less than STARTED
			derivedStateOf {
				val previous = previousLifecycle.value
				val isActive = navigator.isActive
				
				if(previous == Lifecycle.State.RESUMED) {
					if(isActive) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
				} else {
					previous
				}
			}
		}
		val debugContext = rememberDebugContext(contextName = route.name ?: "(ComposeRoute)")
		
		CompositionLocalProvider(
			sLocalCurrentNavigator provides navigator,
			LocalLifecycleState provides lifecycle,
			
			LocalSnackbarHost provides remember { SnackbarHostState() },
			LocalDebugContext provides debugContext
		) {
			Box {
				assertConstant(route.content)
				route.content!!()
				SnackbarHost(remember { SnackbarHostState() }, modifier = Modifier.safeContentPadding())
			}
		}
	}
}
