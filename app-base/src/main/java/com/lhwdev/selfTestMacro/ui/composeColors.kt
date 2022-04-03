package com.lhwdev.selfTestMacro.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance


val Colors.primarySurfaceColored: Color
	@Composable get() = if(isLight) primary else lerp(surface, primary, .3f)

val Colors.primaryContainer: Color
	@Composable get() = lerp(surface, primary, .2f)

/**
 * A color that is used for texts, icons, and small elements on the background or surface.
 * This color is primary in light mode, and brightened primary color in dark mode.
 */
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


val Color.isDark: Boolean get() = luminance() < 0.5f
