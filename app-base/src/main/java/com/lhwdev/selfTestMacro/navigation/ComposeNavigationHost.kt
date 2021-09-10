package com.lhwdev.selfTestMacro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.lhwdev.selfTestMacro.ui.EnableAutoSystemUi
import com.lhwdev.selfTestMacro.ui.utils.AnimateListAsComposable


@Composable
fun ComposeNavigationHost(navigator: Navigator) {
	CompositionLocalProvider(
		LocalGlobalNavigator provides navigator
	) {
		AnimateListAsComposable(
			navigator.routes,
			isOpaque = { it.isOpaque },
			animation = { route, visible, onAnimationEnd, content ->
				val transition = route as? RouteTransition ?: DefaultTransition(isOpaque = route.isOpaque)
				transition.Transition(
					route = route,
					visibleState = visible,
					onAnimationEnd = onAnimationEnd,
					content = content
				)
			}
		) { index, route ->
			EnabledRoute(enabled = index == navigator.routes.lastIndex) {
				RouteContent(route)
			}
		}
	}
}

@Composable
private fun EnabledRoute(enabled: Boolean, content: @Composable () -> Unit) {
	EnableAutoSystemUi(enabled) {
		content()
	}
}

