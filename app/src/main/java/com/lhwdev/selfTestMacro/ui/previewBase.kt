package com.lhwdev.selfTestMacro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.Insets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.WindowInsets
import com.google.accompanist.systemuicontroller.SystemUiController


@Composable
fun PreviewBase(statusBar: Boolean = false, content: @Composable () -> Unit) {
	val density = LocalDensity.current
	
	val uiController = remember {
		object : SystemUiController {
			override var isNavigationBarContrastEnforced: Boolean = false
			override var isNavigationBarVisible: Boolean = true
			override var isStatusBarVisible: Boolean = true
			override var navigationBarDarkContentEnabled: Boolean = false
			override var statusBarDarkContentEnabled: Boolean = false
			
			override fun setNavigationBarColor(
				color: Color,
				darkIcons: Boolean,
				navigationBarContrastEnforced: Boolean,
				transformColorForLightContent: (Color) -> Color,
			) {
			}
			
			override fun setStatusBarColor(
				color: Color,
				darkIcons: Boolean,
				transformColorForLightContent: (Color) -> Color,
			) {
			}
		}
	}
	
	AppTheme {
		CompositionLocalProvider(
			LocalPreview provides true,
			LocalGlobalNavigator provides NavigatorImpl(),
			LocalWindowInsets provides remember {
				object : WindowInsets {
					override val ime: WindowInsets.Type = ImmutableWindowInsetsType()
					override val navigationBars: WindowInsets.Type = ImmutableWindowInsetsType()
					override val statusBars: WindowInsets.Type = ImmutableWindowInsetsType(
						layoutInsets = Insets.Insets(top = with(density) { 30.dp.roundToPx() })
					)
					override val systemBars: WindowInsets.Type = ImmutableWindowInsetsType()
					override val systemGestures: WindowInsets.Type = ImmutableWindowInsetsType()
				}
			},
			LocalPreviewUiController provides uiController
		) {
			Box {
				content()
				
				if(statusBar) Row(
					Modifier
						.fillMaxWidth()
						.height(30.dp)
						.padding(15.dp, 5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					val color =
						if(uiController.statusBarDarkContentEnabled) Color.Black else Color.White
					
					Text("12:34", color = color)
					Spacer(Modifier.weight(1f))
					Text("Hello, Preview!", color = color)
				}
			}
		}
	}
}


private class ImmutableWindowInsetsType(
	override val layoutInsets: Insets = Insets.Empty,
	override val animatedInsets: Insets = Insets.Empty,
	override val isVisible: Boolean = false,
	override val animationInProgress: Boolean = false,
	override val animationFraction: Float = 0f,
) : WindowInsets.Type

