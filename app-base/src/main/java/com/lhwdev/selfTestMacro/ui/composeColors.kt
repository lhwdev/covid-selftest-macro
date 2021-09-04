package com.lhwdev.selfTestMacro.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp


val Colors.primaryActive: Color
	@Composable get() = when(val contentColor = LocalContentColor.current) {
		onSurface -> primary
		onBackground -> primary
		else -> lerp(primary, contentColor, 0.9f) // unknown; default
	}

val DefaultContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)


val MediumContentColor: Color
	@Composable get() = LocalContentColor.current.copy(alpha = ContentAlpha.medium)


@Suppress("ComposableNaming")
@Composable
fun Color(onLight: Color, onDark: Color): Color =
	if(MaterialTheme.colors.isLight) onLight else onDark
