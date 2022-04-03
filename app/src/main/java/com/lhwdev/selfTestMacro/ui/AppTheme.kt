package com.lhwdev.selfTestMacro.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver


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
	if(false /* isEasterEgg */) {
		val color = rememberInfiniteTransition()
		val primaryProgress = color.animateFloat(
			0f, 360f, infiniteRepeatable(
				animation = tween(easing = LinearEasing, durationMillis = 7000),
				repeatMode = RepeatMode.Restart
			)
		).value
		
		val primary = Color.hsl(primaryProgress * 4 % 360, 1f, .5f)
		val onSurface = Color.hsl(primaryProgress * 2 % 360, 1f, .2f)
		val background = Color.hsl(primaryProgress, 1f, .95f)
		
		MaterialTheme(
			colors = lightColors(
				primary = primary,
				primaryVariant = Color.Black.copy(alpha = .2f).compositeOver(primary),
				onPrimary = Color(0xffffffff),
				secondary = Color(0xff55acff),
				secondaryVariant = Color(0xff1a90ff),
				onSecondary = Color(0x00000000),
				onSurface = onSurface,
				onBackground = onSurface,
				background = background
			),
			content = content
		)
	} else {
		MaterialTheme(
			colors = if(isSystemInDarkTheme()) sDarkColors else sLightColors,
			content = content
		)
	}
}
