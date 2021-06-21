@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.Insets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.WindowInsets
import com.google.accompanist.systemuicontroller.SystemUiController


typealias Route = MutableList<@Composable () -> Unit>

fun Route.removeRoute(item: @Composable () -> Unit): Boolean {
	println("removeRoute $item")
	val index = lastIndexOf(item)
	if(index == -1) return false
	subList(index, size).clear()
	println("removed ${joinToString { "$it" }}")
	return true
}

val LocalActivity = compositionLocalOf<Activity> { error("not provided") }
val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalRoute = compositionLocalOf<Route> { error("not provided") }
val LocalPreview = staticCompositionLocalOf { false }


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposeApp(activity: Activity) {
	val context = LocalContext.current
	val pref = remember(context) { context.preferenceState }
	val route = remember {
		val route = mutableStateListOf<@Composable () -> Unit>()
		val initialFirst = pref.firstState == 0
		route.add @Composable {
			if(initialFirst) Setup()
			else Main()
		}
		route
	}
	
	val modifier = Modifier.onPreviewKeyEvent { event ->
		val back = event.key == Key.Back && event.type == KeyEventType.KeyDown
		
		if(back && route.size > 1) {
			route.removeAt(route.lastIndex)
			true
		} else false
	}
	
	
	AppTheme {
		CompositionLocalProvider(
			LocalActivity provides activity,
			LocalPreference provides pref,
			LocalRoute provides route
		) {
			ProvideAutoWindowInsets {
				Box(modifier) {
					for((index, routeItem) in route.withIndex()) key(index) {
						routeItem()
					}
				}
			}
		}
		
		BackHandler(route.size > 1) {
			route.removeLast()
		}
	}
}

private class ImmutableWindowInsetsType(
	override val layoutInsets: Insets = Insets.Empty,
	override val animatedInsets: Insets = Insets.Empty,
	override val isVisible: Boolean = false,
	override val animationInProgress: Boolean = false,
	override val animationFraction: Float = 0f,
) : WindowInsets.Type


val LocalPreviewUiController =
	staticCompositionLocalOf<SystemUiController> { error("not provided") }

@Composable
fun PreviewBase(statusBar: Boolean = false, content: @Composable () -> Unit) {
	val density = LocalDensity.current
	
	val uiController = remember {
		object : SystemUiController {
			override var isNavigationBarContrastEnforced: Boolean = false
			override var isNavigationBarVisible: Boolean = true
			override var isStatusBarVisible: Boolean = true
			override var navigationBarDarkContentEnabled: Boolean = false
			override var statusBarDarkContentEnabled: Boolean = false
			
			override fun setNavigationBarColor(
				color: Color,
				darkIcons: Boolean,
				navigationBarContrastEnforced: Boolean,
				transformColorForLightContent: (Color) -> Color,
			) {
			}
			
			override fun setStatusBarColor(
				color: Color,
				darkIcons: Boolean,
				transformColorForLightContent: (Color) -> Color,
			) {
			}
		}
	}
	
	AppTheme {
		CompositionLocalProvider(
			LocalPreview provides true,
			LocalRoute provides remember { mutableStateListOf(@Composable {}) },
			LocalWindowInsets provides remember {
				object : WindowInsets {
					override val ime: WindowInsets.Type = ImmutableWindowInsetsType()
					override val navigationBars: WindowInsets.Type = ImmutableWindowInsetsType()
					override val statusBars: WindowInsets.Type = ImmutableWindowInsetsType(
						layoutInsets = Insets.Insets(top = with(density) { 30.dp.roundToPx() })
					)
					override val systemBars: WindowInsets.Type = ImmutableWindowInsetsType()
					override val systemGestures: WindowInsets.Type = ImmutableWindowInsetsType()
				}
			},
			LocalPreviewUiController provides uiController
		) {
			Box {
				content()
				
				if(statusBar) Row(
					Modifier
						.fillMaxWidth()
						.height(30.dp)
						.padding(15.dp, 5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					val color =
						if(uiController.statusBarDarkContentEnabled) Color.Black else Color.White
					
					Text("12:34", color = color)
					Spacer(Modifier.weight(1f))
					Text("Hello, Preview!", color = color)
				}
			}
		}
	}
}


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
