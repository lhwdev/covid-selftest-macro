package com.lhwdev.selfTestMacro.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp


val Colors.primarySurfaceColored: Color
	@Composable get() = if(isLight) primary else lerp(primary, surface, .7f)

val Colors.primaryActive: Color
	@Composable get() = primaryActive(1f)

@Composable // friction + -> more primary
fun Colors.primaryActive(friction: Float): Color = when(val contentColor = LocalContentColor.current) {
	onSurface, onBackground -> lerp(contentColor, primary, friction)
	else -> lerp(primary, contentColor, friction * .5f + .5f)
}

val DefaultContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)


val MediumContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = ContentAlpha.medium)

val DisabledContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = ContentAlpha.disabled)


@Suppress("ComposableNaming")
@Composable
fun Color(onLight: Color, onDark: Color): Color =
	if(MaterialTheme.colors.isLight) onLight else onDark
