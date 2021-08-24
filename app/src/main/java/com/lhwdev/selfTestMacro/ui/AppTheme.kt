package com.lhwdev.selfTestMacro.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val sLightColors = lightColors(
	primary = Color(0xff2962ff),
	primaryVariant = Color(0xff5000b3),
	onPrimary = Color(0xffffffff),
	secondary = Color(0xff03dac5),
	secondaryVariant = Color(0xff04bfad)
)


@Composable
fun AppTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = sLightColors,
		content = content
	)
}
