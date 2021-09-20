package com.lhwdev.selfTestMacro.ui.pages.splash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.lifecycle.Lifecycle
import com.lhwdev.selfTestMacro.lifecycle.rememberLifecycle
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Route
import com.lhwdev.selfTestMacro.showToast
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.pages.info.showDebugWindow
import com.lhwdev.selfTestMacro.ui.pages.intro.Intro
import com.lhwdev.selfTestMacro.ui.pages.main.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile


@Composable
fun Splash() {
	val pref = LocalPreference.current
	val context = LocalContext.current
	val navigator = LocalNavigator
	
	
	val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
	val progress = animateLottieCompositionAsState(composition)
	
	val lifecycle by rememberLifecycle()
	
	
	Surface(color = MaterialTheme.colors.primarySurface) {
		AutoSystemUi(
			onScreenMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
		) { scrims ->
			scrims.statusBar()
			
			Box {
				LottieAnimation(
					composition = composition,
					progress = progress.value,
					modifier = Modifier.fillMaxSize()
				)
				
				Spacer(
					Modifier.align(Alignment.TopEnd)
						.size(48.dp)
						.clickable {
							context.showToast("디버그 모드 켜짐")
							navigator.showDebugWindow()
						}
				)
			}
			
			scrims.navigationBar()
		}
	}
	
	LaunchedEffect(Unit) {
		delay(2000)
		
		snapshotFlow { navigator.isTop && lifecycle >= Lifecycle.resumed }
			.takeWhile { !it }
			.collect()
		
		val initialFirst = pref.isFirstTime
		
		val content: @Composable () -> Unit =
			if(initialFirst || pref.db.testGroups.groups.isEmpty()) {
				{ Intro() }
			} else {
				{ Main() }
			}
		
		navigator.replaceRoute(Route(transition = FadeRouteTransition(), content = content))
	}
	
}
