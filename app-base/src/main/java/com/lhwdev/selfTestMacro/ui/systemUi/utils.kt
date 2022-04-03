package com.lhwdev.selfTestMacro.ui.systemUi

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lhwdev.selfTestMacro.ui.LocalPreview
import com.lhwdev.selfTestMacro.ui.LocalPreviewUiController

// https://issuetracker.google.com/issues/217770337
val WindowInsets.isVisible: Boolean
	@Composable get() {
		val density = LocalDensity.current
		val direction = LocalLayoutDirection.current
		
		return isVisible(density, direction)
	}

@Composable
fun WindowInsets.rememberIsVisible(): State<Boolean> {
	val pair = rememberUpdatedState(LocalDensity.current to LocalLayoutDirection.current)
	return remember {
		derivedStateOf {
			val (density, direction) = pair.value
			isVisible(density, direction)
		}
	}
}

fun WindowInsets.isVisible(density: Density, direction: LayoutDirection): Boolean {
	return getTop(density) != 0 ||
		getBottom(density) != 0 ||
		getLeft(density, direction) != 0 ||
		getRight(density, direction) != 0
}

val ScrimNavSurfaceColor
	@Composable get() = MaterialTheme.colors.surface.copy(alpha = 0.7f)

@Composable
fun rememberPreviewUiController(): SystemUiController = LocalPreviewUiController.current

@Composable
fun rememberUiController(): SystemUiController = if(LocalPreview.current) {
	rememberPreviewUiController()
} else {
	rememberSystemUiController()
}
