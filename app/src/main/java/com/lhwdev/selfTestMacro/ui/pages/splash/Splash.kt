package com.lhwdev.selfTestMacro.ui.pages.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Route
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.pages.intro.Intro
import com.lhwdev.selfTestMacro.ui.pages.main.Main
import kotlinx.coroutines.delay


@Composable
fun Splash() {
	val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
	val progress = animateLottieCompositionAsState(composition)
	
	val pref = LocalPreference.current
	val navigator = LocalNavigator
	
	AutoSystemUi(
		onScreenMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
	) { scrims ->
		Surface(color = MaterialTheme.colors.primarySurface) {
			scrims.statusBar()
			
			LottieAnimation(
				composition = composition,
				progress = progress.value,
				modifier = Modifier.fillMaxSize()
			)
			
			scrims.navigationBar()
		}
	}
	
	LaunchedEffect(Unit) {
		delay(2000)
		
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
