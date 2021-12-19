package com.lhwdev.selfTestMacro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val sLightColors = lightColors(
	primary = Color(0xff376cff),
	primaryVariant = Color(0xff0044ff),
	onPrimary = Color(0xffffffff),
	secondary = Color(0xff55acff),
	secondaryVariant = Color(0xff1a90ff),
	onSecondary = Color(0x00000000)
)

private val sDarkColors = darkColors(
	primary = Color(0xff376cff),
	primaryVariant = Color(0xff0044ff),
	onPrimary = Color(0xffffffff),
	secondary = Color(0xff55acff),
	secondaryVariant = Color(0xff1a90ff),
	onSecondary = Color(0x00000000)
)


@Composable
fun AppTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = if(isSystemInDarkTheme()) sDarkColors else sLightColors,
		content = content
	)
}
