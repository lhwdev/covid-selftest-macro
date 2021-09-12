package com.lhwdev.selfTestMacro.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp


val Colors.primaryActive: Color
	@Composable get() = primaryActive(1f)

@Composable // friction + -> more primary
fun Colors.primaryActive(friction: Float): Color = when(val contentColor = LocalContentColor.current) {
	onSurface, onBackground -> lerp(contentColor, primary, friction)
	else -> lerp(primary, contentColor, friction * 0.5f + 0.5f) // to ensure legibility
}

val DefaultContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)


val MediumContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = ContentAlpha.medium)


@Suppress("ComposableNaming")
@Composable
fun Color(onLight: Color, onDark: Color): Color =
	if(MaterialTheme.colors.isLight) onLight else onDark
