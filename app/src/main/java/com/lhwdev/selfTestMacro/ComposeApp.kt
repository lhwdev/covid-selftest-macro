@file:JvmName("AppKt")

package com.lhwdev.selfTestMacro

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.AmbientContext


val AmbientActivity = ambientOf<Activity>()
val AmbientPreference = ambientOf<PreferenceState>()
val AmbientRoute = ambientOf<SnapshotStateList<@Composable () -> Unit>>()


@Composable
fun ComposeApp(activity: Activity) {
	val context = AmbientContext.current
	val pref = remember(context) { context.preferenceState }
	val route = remember {
		val route = SnapshotStateList<@Composable () -> Unit>()
		route.add { Main() }
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
			Providers(
				AmbientActivity provides activity,
				AmbientPreference provides pref,
				AmbientRoute provides route
			) {
				AmbientRoute.current.last().invoke()
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
			secondary = Color(0xff03dac5),
			secondaryVariant = Color(0xff04bfad)
		),
		content = content
	)
}
