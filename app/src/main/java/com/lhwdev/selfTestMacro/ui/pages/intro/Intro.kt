package com.lhwdev.selfTestMacro.ui.pages.intro

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.navigation.FadeRouteTransition
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Route
import com.lhwdev.selfTestMacro.navigation.RouteTransitionBlock
import com.lhwdev.selfTestMacro.ui.DefaultContentColor
import com.lhwdev.selfTestMacro.ui.PreviewBase
import com.lhwdev.selfTestMacro.ui.pages.setup.SetupParameters
import com.lhwdev.selfTestMacro.ui.pages.setup.SetupRoute
import com.lhwdev.selfTestMacro.ui.primarySurfaceColored
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.OnScreenSystemUiMode


val IntroRoute: Route = Route("Intro") { Intro() }

@Preview
@Composable
fun IntroPreview() {
	PreviewBase { Intro() }
}


@Composable
private fun Intro() {
	val navigator = LocalNavigator
	
	Surface(
		color = MaterialTheme.colors.primarySurfaceColored,
		contentColor = contentColorFor(MaterialTheme.colors.primary)
	) {
		AutoSystemUi(onScreen = OnScreenSystemUiMode.Immersive()) { scrims ->
			scrims.statusBars()
			
			Column(Modifier.weight(1f).padding(32.dp)) {
				Spacer(Modifier.weight(5f))
				
				Text("코로나19\n자가진단 매크로", style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold)
				
				Spacer(Modifier.weight(3f))
				
				Text("이 앱의 개발자는 이 앱을 이용하면서 생기는 문제의 책임을 지지 않습니다.")
				
				Spacer(Modifier.height(16.dp))
				
				Text("개인정보 처리 방침을 읽고 동의해주세요.")
				Button(
					onClick = {
						navigator.showPrivacyPolicy()
					},
					colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface),
					modifier = Modifier.padding(vertical = 20.dp)
				) {
					Text("개인정보 처리 방침")
				}
				
				Spacer(Modifier.weight(4f))
				
				Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
					TextButton(
						onClick = {
							navigator.replaceRoute(
								SetupRoute(SetupParameters(initial = true)).withExtras(
									RouteTransitionBlock to FadeRouteTransition(animationSpec = tween(durationMillis = 500))
								)
							)
						},
						colors = ButtonDefaults.textButtonColors(contentColor = DefaultContentColor),
						contentPadding = PaddingValues(all = 12.dp)
					) { Text("동의") }
				}
			}
			
			scrims.navigationBars()
		}
	}
}
