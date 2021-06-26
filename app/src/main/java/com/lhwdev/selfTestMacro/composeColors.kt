package com.lhwdev.selfTestMacro

import androidx.compose.material.Colors
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
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

