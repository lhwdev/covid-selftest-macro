package com.lhwdev.selfTestMacro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.ListContent
import com.vanpra.composematerialdialogs.Title
import com.vanpra.composematerialdialogs.showDialogAsync


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
					IconButton(onClick = { navigator.popRoute() }) {
						Icon(
							painterResource(R.drawable.ic_arrow_left_24),
							contentDescription = "back"
						)
					}
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
				Text(
					BuildConfig.VERSION_NAME,
					style = MaterialTheme.typography.h4,
					color = MediumContentColor
				)
				
				Spacer(Modifier.weight(1f))
				
				Spacer(Modifier.weight(2f))
				
				Row {
					Text("개발자: ")
					Text(
						"lhwdev (이현우)",
						style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
						modifier = Modifier.clickable {
							context.openWebsite("https://github.com/lhwdev")
						}
					)
				}
				
				Text("Thanks to 이승수")
				
				Spacer(Modifier.height(16.dp))
				
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
							context.openWebsite("https://github.com/lhwdev/covid-selftest-macro/discussions")
						}) { Text("깃허브 커뮤니티") }
					}
					
					if(pref.isDebugEnabled) {
						Spacer(Modifier.height(8.dp))
						
						RoundButton(
							onClick = {
								navigator.showDialogAsync {
									Title { Text("개발자 모드") }
									ListContent {
										Column {
											ListItem(
												modifier = Modifier.clickable {
													pref.isDebugEnabled = false
												}
											) {
												Text("개발자 모드 끄기")
											}
											
											ListItem { Text("에러 로깅 활성화됨") }
										}
									}
								}
							},
							colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x88555555)),
							contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
						) {
							Text("개발자 모드 ${if(context.isDebugEnabled) "켜짐" else "꺼짐"}")
						}
					}
				}
				
				Spacer(Modifier.height(12.dp))
			}
		}
	}
}
