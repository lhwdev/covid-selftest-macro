package com.lhwdev.selfTestMacro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val sLightColors = lightColors(
	primary = Color(0xff2962ff),
	primaryVariant = Color(0xff5000b3),
	onPrimary = Color(0xffffffff),
	secondary = Color(0xff2962ff),
	secondaryVariant = Color(0xff5000b3)
)

private val sDarkColors = darkColors(
	primary = Color(0xff2962ff),
	primaryVariant = Color(0xff5000b3),
	onPrimary = Color(0xffffffff),
	secondary = Color(0xff2962ff),
	secondaryVariant = Color(0xff5000b3)
)


@Composable
fun AppTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = if(isSystemInDarkTheme()) sDarkColors else sLightColors,
		content = content
	)
}
