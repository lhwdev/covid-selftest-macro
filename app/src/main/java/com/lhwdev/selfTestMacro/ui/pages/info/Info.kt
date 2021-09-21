package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhwdev.fetch.toJson
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.BuildConfig
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.openWebsite
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.MediumContentColor
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.common.LinkedText
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.pages.intro.showPrivacyPolicy
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.vanpra.composematerialdialogs.Content
import com.vanpra.composematerialdialogs.Title
import com.vanpra.composematerialdialogs.showDialogAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val sInfoDeveloper = "info/developer.json"


@Composable
fun Info(): Unit = MaterialTheme(
	colors = MaterialTheme.colors.copy(primary = Color(0xff304ffe), onPrimary = Color.White)
) {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	Surface(color = MaterialTheme.colors.primarySurface) {
		AutoSystemUi(
			onScreenMode = OnScreenSystemUiMode.Opaque(Color.Transparent)
		) {
			TopAppBar(
				navigationIcon = {
					SimpleIconButton(
						icon = R.drawable.ic_arrow_back_24, contentDescription = "뒤로 가기",
						onClick = { navigator.popRoute() }
					)
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
				Spacer(Modifier.height(12.dp))
				
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
						onClick = {
							scope.launch {
								val data = withContext(Dispatchers.IO) {
									try {
										App.metaBranch.getContent(sInfoDeveloper)
											.toJson(InfoUserStructure.Detail.serializer(), anyContentType = true)
									} catch(th: Throwable) {
										navigator.showDialogAsync {
											Title { Text("정보를 불러오지 못했습니다.") }
											Content { Text(th.stackTraceToString()) }
										}
										null
									}
								} ?: return@launch
								
								navigator.showDialogAsync(maxHeight = Dp.Infinity) {
									InfoUsersDetail(data)
								}
							}
						}
					)
				}
				
				Spacer(Modifier.height(8.dp))
				
				InfoUsers()
				
				Spacer(Modifier.height(8.dp))
				
				LinkedText(
					"오픈소스 라이센스",
					onClick = { navigator.pushRoute { OpenSources() } }
				)
				
				Spacer(Modifier.height(28.dp))
				
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
