package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.BuildConfig
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.openWebsite
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.MediumContentColor
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.common.BackButton
import com.lhwdev.selfTestMacro.ui.common.LinkedText
import com.lhwdev.selfTestMacro.ui.pages.intro.showPrivacyPolicy
import com.lhwdev.selfTestMacro.ui.utils.RoundButton


@Composable
fun Info(): Unit = MaterialTheme(
	colors = MaterialTheme.colors.copy(primary = Color(0xff304ffe), onPrimary = Color.White)
) {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	val context = LocalContext.current
	
	Surface(color = MaterialTheme.colors.primarySurface) {
		AutoSystemUi(
			onScreenMode = OnScreenSystemUiMode.Opaque(Color.Transparent)
		) {
			TopAppBar(
				navigationIcon = {
					BackButton { navigator.popRoute() }
				},
				title = {},
				actions = {
					IconButton(onClick = { pref.isDebugEnabled = true }) {}
				},
				backgroundColor = Color.Transparent,
				elevation = 0.dp
			)
			
			Column(
				modifier = Modifier
					.padding(24.dp)
					.fillMaxSize()
			) {
				Spacer(Modifier.weight(4f))
				
				Text("자가진단 매크로", style = MaterialTheme.typography.h3)
				Spacer(Modifier.height(8.dp))
				
				Text(
					BuildConfig.VERSION_NAME,
					style = MaterialTheme.typography.h5,
					color = MediumContentColor
				)
				
				Spacer(Modifier.weight(1f))
				
				Spacer(Modifier.weight(2f))
				
				Row {
					Text("개발자: ")
					LinkedText(
						"lhwdev (이현우)",
						onClick = { context.openWebsite("https://github.com/lhwdev") }
					)
				}
				
				Text("Thanks to 이승수")
				Row {
					Text("Splash design by ")
					LinkedText(
						"권순엽",
						onClick = { context.openWebsite("https://agar.io/") }
					)
				}
				
				Spacer(Modifier.height(16.dp))
				
				LinkedText(
					"오픈소스 라이센스",
					onClick = { navigator.pushRoute { OpenSources() } }
				)
				
				Spacer(Modifier.height(8.dp))
				
				LinkedText(
					"개인정보 처리방침",
					onClick = { navigator.showPrivacyPolicy() }
				)
				
				Spacer(Modifier.weight(8.5f))
				
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.CenterHorizontally),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Row {
						RoundButton(onClick = {
							context.openWebsite("https://github.com/lhwdev/covid-selftest-macro")
						}) { Text("공식 웹사이트") }
						
						Spacer(Modifier.width(10.dp))
						
						RoundButton(onClick = {
							context.openWebsite("https://discord.link/hcs")
						}) { Text("디스코드 서버") }
					}
					
					if(pref.isDebugEnabled) {
						Spacer(Modifier.height(8.dp))
						
						RoundButton(
							onClick = {
								navigator.showDebugWindow()
							},
							colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x88555555)),
							contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
						) {
							Text("개발자 모드 ${if(pref.isDebugEnabled) "켜짐" else "꺼짐"}")
						}
					}
				}
				
				Spacer(Modifier.height(12.dp))
			}
		}
	}
}
