package com.lhwdev.selfTestMacro

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.*
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController


fun Modifier.autoPadding(
	statusBar: Boolean = true
): Modifier = composed(inspectorInfo = debugInspectorInfo {
	name = "autoPadding"
	properties["statusBar"] = statusBar
}) {
	val insets = LocalWindowInsets.current
	
	padding(
		rememberInsetsPaddingValues(
			derivedWindowInsetsTypeOf(
				*listOfNotNull(
					if(statusBar) insets.statusBars else null,
					insets.navigationBars,
					insets.ime
				).toTypedArray()
			)
		)
	)
}


val ScrimLightColor = Color.White.copy(alpha = 0.1f)
val ScrimDarkColor = Color.Black.copy(alpha = 0.1f)
val ScrimDarkerColor = Color.Black.copy(alpha = 0.17f)

sealed interface SystemUiMode {
	object Default : SystemUiMode
}

sealed interface OnScreenSystemUiMode : SystemUiMode {
	class Immersive(val scrimColor: Color = ScrimDarkColor) : OnScreenSystemUiMode
	object Opaque : OnScreenSystemUiMode
}


class Scrims(
	val statusBar: @Composable () -> Unit,
	val navigationBar: @Composable () -> Unit
)

private fun Color.isDarkColor(): Boolean = luminance() < 0.5f


@Composable
fun rememberPreviewUiController(): SystemUiController = LocalPreviewUiController.current


val isSystemUiDarkContentAvailable: Boolean = Build.VERSION.SDK_INT >= 23


@Composable
fun PreviewSideEffect(effect: () -> Unit) {
	if(LocalPreview.current) effect()
	else SideEffect(effect)
}

// note that this does not clean-up
@Composable
fun AutoSystemUi(
	enabled: Boolean,
	statusBar: OnScreenSystemUiMode? = null,
	navigationBar: OnScreenSystemUiMode? = OnScreenSystemUiMode.Opaque,
	ime: SystemUiMode? = SystemUiMode.Default,
	content: @Composable (Scrims) -> Unit
) {
	val controller = if(LocalPreview.current) {
		rememberPreviewUiController()
	} else {
		rememberSystemUiController()
	}
	
	val scrims = remember {
		Scrims(
			statusBar = {
				if(statusBar is OnScreenSystemUiMode.Immersive) {
					Box(Modifier.background(statusBar.scrimColor).statusBarsHeight())
					val isDark = LocalContentColor.current.isDarkColor()
					if(enabled) PreviewSideEffect {
						controller.statusBarDarkContentEnabled = isDark
					}
				}
			},
			navigationBar = {
				if(navigationBar is OnScreenSystemUiMode.Immersive) {
					Box(Modifier.background(navigationBar.scrimColor).navigationBarsHeight())
					val isDark = LocalContentColor.current.isDarkColor()
					if(enabled) PreviewSideEffect {
						controller.navigationBarDarkContentEnabled = isDark
					}
				}
			}
		)
	}
	
	var modifier: Modifier = Modifier
	if(ime is SystemUiMode.Default) modifier = modifier.imePadding()
	if(statusBar is OnScreenSystemUiMode.Opaque) modifier = modifier.statusBarsPadding()
	if(navigationBar is OnScreenSystemUiMode.Opaque) modifier = modifier.navigationBarsPadding()
	
	Box(modifier) {
		content(scrims)
	}
}

@Composable
fun TopAppBar(
	title: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	statusBarScrim: @Composable () -> Unit,
	navigationIcon: @Composable (() -> Unit)? = null,
	actions: @Composable RowScope.() -> Unit = {},
	backgroundColor: Color = MaterialTheme.colors.primarySurface,
	contentColor: Color = contentColorFor(backgroundColor),
	elevation: Dp = AppBarDefaults.TopAppBarElevation
) {
	Surface(
		color = backgroundColor,
		elevation = elevation
	) {
		Column {
			statusBarScrim()
			
			TopAppBar(
				title = title,
				modifier = modifier,
				navigationIcon = navigationIcon,
				actions = actions,
				backgroundColor = Color.Transparent,
				contentColor = contentColor,
				elevation = 0.dp
			)
		}
	}
}
