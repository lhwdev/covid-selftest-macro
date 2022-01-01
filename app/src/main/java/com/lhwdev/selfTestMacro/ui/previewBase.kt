package com.lhwdev.selfTestMacro.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.Insets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.WindowInsets
import com.google.accompanist.systemuicontroller.SystemUiController
import com.lhwdev.selfTestMacro.FirstInitialization
import com.lhwdev.selfTestMacro.MainApplication
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.DefaultDebugManager
import com.lhwdev.selfTestMacro.debug.LocalDebugContext
import com.lhwdev.selfTestMacro.debug.UiDebugContext
import com.lhwdev.selfTestMacro.navigation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


@Composable
fun PreviewBase(statusBar: Boolean = false, content: @Composable () -> Unit) {
	PreviewBase(statusBar, Route(content = content))
}

@Composable
fun PreviewBase(statusBar: Boolean = false, route: Route) {
	val navigator = remember {
		NavigatorImpl().also { it.pushRoute(route) }
	}
	
	// val currentRoute by rememberUpdatedState(route)
	//
	// LaunchedEffect(Unit) {
	// 	snapshotFlow { currentRoute }
	// 		.drop(1)
	// 		.collect {
	// 			navigator.clearRoute()
	// 			navigator.pushRoute(it)
	// 		}
	// }
	
	PreviewBase(statusBar, navigator)
}

@Composable
fun PreviewBase(statusBar: Boolean = false, navigator: Navigator) {
	val density = LocalDensity.current
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	if(changed(Unit)) initializeStubApp(context)
	
	val debugContext = remember {
		val manager = DefaultDebugManager(androidContext = context, workScope = CoroutineScope(Dispatchers.Default))
		
		UiDebugContext(
			manager = manager,
			context = context,
			contextName = "Compose Preview",
			flags = DebugContext.DebugFlags(enabled = true, debuggingWithIde = true),
			showErrorInfo = { _, _, _ -> },
			uiContext = scope.coroutineContext
		)
	}
	
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
			LocalDebugContext provides debugContext,
			LocalGlobalNavigator provides navigator,
			LocalWindowInsets provides remember {
				object : WindowInsets {
					override val ime: WindowInsets.Type = ImmutableWindowInsetsType()
					override val navigationBars: WindowInsets.Type = ImmutableWindowInsetsType()
					override val statusBars: WindowInsets.Type = ImmutableWindowInsetsType(
						layoutInsets = Insets.Insets(top = with(density) { 30.dp.roundToPx() })
					)
					override val systemBars: WindowInsets.Type = ImmutableWindowInsetsType()
					override val systemGestures: WindowInsets.Type = ImmutableWindowInsetsType()
					override val displayCutout: WindowInsets.Type = ImmutableWindowInsetsType()
				}
			},
			LocalPreviewUiController provides uiController
		) {
			Box {
				ComposeNavigationHost(navigator)
				
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

private fun initializeStubApp(context: Context) {
	with(FirstInitialization) {
		context.initializeApplication(
			versionName = "1.0.0-preview",
			versionCode = 10000000,
			flavor = "preview",
			debug = true,
			appIconForeground = R.mipmap.ic_launcher_foreground,
			appIcon = R.mipmap.ic_launcher,
			mainActivity = MainApplication::class.java
		)
	}
}


private class ImmutableWindowInsetsType(
	override val layoutInsets: Insets = Insets.Empty,
	override val animatedInsets: Insets = Insets.Empty,
	override val isVisible: Boolean = false,
	override val animationInProgress: Boolean = false,
	override val animationFraction: Float = 0f,
) : WindowInsets.Type

