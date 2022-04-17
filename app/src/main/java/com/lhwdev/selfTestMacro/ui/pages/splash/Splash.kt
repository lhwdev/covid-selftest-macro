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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.RouteTransitionBlock
import com.lhwdev.selfTestMacro.showToast
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.pages.info.showDebugWindow
import com.lhwdev.selfTestMacro.ui.pages.intro.IntroRoute
import com.lhwdev.selfTestMacro.ui.pages.main.MainRoute
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.utils.LocalLifecycleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile


@Composable
fun Splash() {
	val pref = LocalPreference.current
	val context = LocalContext.current
	val navigator = LocalNavigator
	
	
	val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
	val progress = animateLottieCompositionAsState(
		composition = composition,
		speed = 1.3f
	)
	
	val lifecycle by LocalLifecycleState.current
	
	
	Surface(color = MaterialTheme.colors.primarySurface) {
		AutoSystemUi(
			onScreen = OnScreenSystemUiMode.Immersive()
		) { scrims ->
			scrims.statusBars()
			
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
			
			scrims.navigationBars()
		}
	}
	
	LaunchedEffect(Unit) {
		delay(1300)
		
		snapshotFlow { navigator.isTop && lifecycle >= Lifecycle.State.RESUMED }
			.takeWhile { !it }
			.collect()
		
		val initialFirst = pref.isFirstTime
		
		val route = if(initialFirst || pref.db.testGroups.groups.isEmpty()) {
			IntroRoute
		} else {
			MainRoute
		}
		
		navigator.replaceRoute(route.withExtras(RouteTransitionBlock to FadeRouteTransition()))
	}
	
}
