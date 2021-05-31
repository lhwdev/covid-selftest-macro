@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext


val LocalActivity = compositionLocalOf<Activity> { error("not provided") }
val LocalPreference = compositionLocalOf<PreferenceState> { error("not provided") }
val LocalRoute =
	compositionLocalOf<SnapshotStateList<@Composable () -> Unit>> { error("not provided") }


@Composable
fun ComposeApp(activity: Activity) {
	val context = LocalContext.current
	val pref = remember(context) { context.preferenceState }
	val route = remember {
		val route = SnapshotStateList<@Composable () -> Unit>()
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
	
	Box(modifier) {
		AppTheme {
			CompositionLocalProvider(
				LocalActivity provides activity,
				LocalPreference provides pref,
				LocalRoute provides route
			) {
				route.forEachIndexed { index, routeItem ->
					key(index) {
						routeItem() // be aware!
					}
				}
			}
		}
	}
}

@Composable
fun PreviewBase(content: @Composable () -> Unit) {
	AppTheme {
		content()
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
