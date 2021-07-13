@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext


typealias Route = MutableList<@Composable () -> Unit>

fun Route.removeRoute(item: @Composable () -> Unit): Boolean {
	val index = lastIndexOf(item)
	if(index == -1) return false
	subList(index, size).clear()
	return true
}

val LocalActivity = compositionLocalOf<Activity> { error("not provided") }
val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalRoute = compositionLocalOf<Route> { error("not provided") }
val LocalPreview = staticCompositionLocalOf { false }


@Composable
fun ComposeApp(activity: Activity) {
	val composer = currentComposer
	
	// inject coroutine context
	if(BuildConfig.DEBUG) remember {
		val context = composer::class.java.getDeclaredField("parentContext").also {
			it.isAccessible = true
		}.get(composer) as Recomposer
		
		val contextField = Recomposer::class.java.getDeclaredField("effectCoroutineContext").also {
			it.isAccessible = true
		}
		contextField.set(
			context,
			(contextField.get(context) as CoroutineContext) + CoroutineExceptionHandler { _, th ->
				Log.e("ComposeApp", "Uncaught exception", th)
				throw th
			})
		
		0
	}
	
	val context = LocalContext.current
	val pref = remember(context) { context.preferenceState }
	val route = remember {
		val route = mutableStateListOf<@Composable () -> Unit>()
		val initialFirst = pref.firstState == 0
		route.add @Composable {
			if(initialFirst || pref.db.testGroups.groups.isEmpty()) Setup()
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
					AnimateListAsComposable(route) { index, item ->
						EnabledRoute(enabled = index == route.lastIndex) {
							item()
						}
					}
				}
			}
		}
		
		BackHandler(route.size > 1) {
			route.removeLast()
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
