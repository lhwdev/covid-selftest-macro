package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhwdev.fetch.toJson
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.openWebsite
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.pages.intro.showPrivacyPolicy
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.lhwdev.utils.rethrowIfNeeded
import com.vanpra.composematerialdialogs.Content
import com.vanpra.composematerialdialogs.Title
import com.vanpra.composematerialdialogs.showDialogAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
				modifier = Modifier.fillMaxSize()
			) {
				Spacer(Modifier.weight(4f))
				
				// Header
				Column(Modifier.padding(24.dp)) {
					Text(
						"자가진단 매크로",
						style = MaterialTheme.typography.h3,
						color = with(MaterialTheme.colors) { if(isLight) onPrimary else lerp(onSurface, primary, .2f) }
					)
					Spacer(Modifier.height(12.dp))
					
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						// modifier = Modifier.clickable(
						// 	interactionSource = remember { MutableInteractionSource() },
						// 	indication = null
						// ) {}
					) {
						Text(App.version.toString(), style = MaterialTheme.typography.h5, color = MediumContentColor)
						Text(App.flavor, style = MaterialTheme.typography.h6, color = MediumContentColor)
					}
				}
				
				Spacer(Modifier.weight(3f))
				
				ListItem(Modifier.clickable {
					scope.launch {
						val data = withContext(Dispatchers.Default) {
							try {
								App.github.meta.developerInfo.get()
									.toJson(InfoUserStructure.Detail.serializer(), anyContentType = true)
							} catch(th: Throwable) {
								th.rethrowIfNeeded()
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
				}) {
					val text = buildAnnotatedString {
						append("개발자: ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("lhwdev (이현우)") }
					}
					Text(text)
				}
				
				ListItem(Modifier.clickable { navigator.showSpecialThanks() }) { Text("개발에 도움을 주신 분들") }
				
				
				ListItem(
					modifier = Modifier.clickable { navigator.pushRoute { OpenSources() } }
				) { Text("오픈소스 라이센스") }
				
				ListItem(
					modifier = Modifier.clickable { navigator.showPrivacyPolicy() }
				) { Text("개인정보 처리방침") }
				
				Spacer(Modifier.weight(8.5f))
				
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(12.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Row {
						RoundButton(onClick = {
							context.openWebsite("https://github.com/lhwdev/covid-selftest-macro")
						}) { Text("공식 웹사이트") }
						
						Spacer(Modifier.width(10.dp))
						
						RoundButton(onClick = {
							context.openWebsite("https://discord.io/hcs-macro")
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
			}
		}
	}
}
