@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.SystemUiController


val LocalActivity = compositionLocalOf<Activity> { error("not provided") }
val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalPreview = staticCompositionLocalOf { false }


@Composable
fun ComposeApp(activity: Activity) {
	val context = LocalContext.current
	val pref = remember(context) { context.preferenceState }
	val navigator = remember {
		val navigator = NavigatorImpl()
		
		val initialFirst = pref.isFirstTime
		
		if(initialFirst || pref.db.testGroups.groups.isEmpty()) navigator.pushRoute { Setup() }
		else navigator.pushRoute { Main() }
		
		navigator
	}
	
	AppTheme {
		CompositionLocalProvider(
			LocalActivity provides activity,
			LocalPreference provides pref,
			LocalGlobalNavigator provides navigator
		) {
			ProvideAutoWindowInsets {
				BoxWithConstraints {
					AnimateListAsComposable(
						navigator.routes,
						isOpaque = { it.isOpaque },
						animation = { route, fraction, content ->
							val transition = (route as? RouteTransition) ?: DefaultRouteTransition
							transition.OnTransition(fraction, content)
						}
					) { index, route ->
						EnabledRoute(enabled = index == navigator.routes.lastIndex) {
							RouteContent(route)
						}
					}
				}
			}
		}
		
		BackHandler(navigator.size > 1) {
			navigator.popLastRoute()
		}
	}
}

@Composable
private fun EnabledRoute(enabled: Boolean, content: @Composable () -> Unit) {
	EnableAutoSystemUi(enabled) {
		content()
	}
}

val LocalPreviewUiController =
	staticCompositionLocalOf<SystemUiController> { error("not provided") }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = lightColors(
			primary = Color(0xff6200ee),
			primaryVariant = Color(0xff3700b3),
			onPrimary = Color(0xffffffff),
			secondary = Color(0xff03dac5),
			secondaryVariant = Color(0xff04bfad)
		),
		content = content
	)
}
