package com.lhwdev.selfTestMacro.navigation

import android.app.Dialog
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.lhwdev.selfTestMacro.modules.app_base.R
import com.lhwdev.selfTestMacro.ui.EnableAutoSystemUi
import com.lhwdev.selfTestMacro.ui.utils.AnimateListAsComposable
import com.lhwdev.selfTestMacro.ui.utils.rememberLoopLinkedList
import com.vanpra.composematerialdialogs.FullScreenDialog


private data class RouteInfo(val hasDialog: Boolean, val navigator: CurrentNavigator)


@Composable
fun ComposeNavigationHost(navigator: Navigator) {
	CompositionLocalProvider(
		LocalGlobalNavigator provides navigator
	) {
		val routes = navigator.routes
		
		AnimateListAsComposable(
			routes,
			isOpaque = { it.route[IsOpaque] },
			animation = { route, visible, onAnimationEnd, content ->
				val transition = route.route[RouteTransitionBlock]
				transition.Transition(
					route = route.route,
					visibleState = visible,
					onAnimationEnd = onAnimationEnd,
					content = content
				)
			}
		) { entries, innerContent ->
			val routeInfoList = rememberLoopLinkedList<RouteInfo>(size = entries.size)
			innerContent { index, routeInstance, visible, container ->
				val route = routeInstance.route
				val isDialog = route[IsDialogRoute]
				
				val previous = routeInfoList.previous(index)
				
				val hasDialog = previous?.hasDialog == true
				val parentNavigator = previous?.navigator
				
				
				if(route.content == null) {
					println("[ComposeNavigationHost] warning: no content for $routeInstance!")
					return@innerContent
				}
				
				val scope = rememberCoroutineScope()
				val currentNavigator = remember(navigator) { // route itself is key of AnimateListAsComposable
					CurrentNavigator(
						rootNavigator = navigator,
						currentRouteInstance = routeInstance,
						coroutineScope = scope
					)
				}
				currentNavigator.updateState(parent = parentNavigator, isVisible = visible)
				
				routeInfoList.updateCurrent(
					index, RouteInfo(
						hasDialog = hasDialog || isDialog,
						navigator = currentNavigator
					)
				)
				
				val content = @Composable {
					container {
						EnabledRoute(route = route, enabled = index == entries.lastIndex) {
							RouteContent(currentNavigator)
						}
					}
				}
				
				if(!isDialog && hasDialog) { // to avoid routes hidden below Dialog
					FullScreenDialog(onDismissRequest = { navigator.removeRoute(routeInstance) }, solid = true) {
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
						
						content()
					}
				} else {
					content()
				}
			}
		}
	}
}

@Composable
private fun EnabledRoute(route: Route, enabled: Boolean, content: @Composable () -> Unit) {
	EnableAutoSystemUi(enabled) {
		content()
	}
}
