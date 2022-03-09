package com.lhwdev.selfTestMacro.navigation

import android.app.Dialog
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.lhwdev.selfTestMacro.modules.app_base.R
import com.lhwdev.selfTestMacro.ui.EnableAutoSystemUi
import com.lhwdev.selfTestMacro.ui.ProvideAutoWindowInsets
import com.lhwdev.selfTestMacro.ui.utils.AnimateListAsComposable
import com.vanpra.composematerialdialogs.FullScreenDialog


@Composable
fun ComposeNavigationHost(navigator: Navigator) {
	CompositionLocalProvider(
		LocalGlobalNavigator provides navigator
	) {
		var hasDialog = false
		var parentNavigator: CurrentNavigator? = null
		
		AnimateListAsComposable(
			navigator.routes,
			isOpaque = { it.route.isOpaque },
			animation = { route, visible, onAnimationEnd, content ->
				val transition = route.route as? RouteTransition ?: DefaultTransition(isOpaque = route.route.isOpaque)
				transition.Transition(
					route = route.route,
					visibleState = visible,
					onAnimationEnd = onAnimationEnd,
					content = content
				)
			}
		) { index, route, visible, container ->
			if(route.route is DialogRoute) hasDialog = true
			
			val scope = rememberCoroutineScope()
			val currentNavigator = remember(navigator) { // route itself is key of AnimateListAsComposable
				CurrentNavigator(
					rootNavigator = navigator,
					currentRouteInstance = route,
					coroutineScope = scope,
					parent = parentNavigator
				)
			}
			currentNavigator.parent = parentNavigator
			parentNavigator = currentNavigator
			
			val content = @Composable {
				container {
					EnabledRoute(enabled = index == navigator.routes.lastIndex) {
						RouteContent(currentNavigator)
					}
				}
			}
			
			if(route.route !is DialogRoute && hasDialog) { // to avoid routes hidden below Dialog
				FullScreenDialog(onDismissRequest = { navigator.removeRoute(route) }, solid = true) {
					val dialogLayout = LocalView.current
					
					DisposableEffect(visible) {
						// such a dirty workaround!
						val dialog = (dialogLayout.parent as View).getTag(R.id.FullScreenDialog_Dialog) as Dialog
						if(visible) {
							dialog.show()
						} else {
							dialog.hide()
						}
						onDispose {}
					}
					
					ProvideAutoWindowInsets {
						content()
					}
				}
			} else {
				content()
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

