package com.lhwdev.selfTestMacro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp


@Composable
fun Info(): Unit = MaterialTheme(
	colors = MaterialTheme.colors.copy(primary = Color(0xff304ffe), onPrimary = Color.White)
) {
	val context = LocalContext.current
	
	Surface(color = MaterialTheme.colors.primarySurface) {
		AutoSystemUi(
			onScreenMode = OnScreenSystemUiMode.Opaque(Color.Transparent)
		) {
			Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
				Spacer(Modifier.weight(4f))
				
				Text("자가진단 매크로", style = MaterialTheme.typography.h3)
				Text(
					BuildConfig.VERSION_NAME,
					style = MaterialTheme.typography.h4,
					color = MediumContentColor
				)
				
				Spacer(Modifier.weight(2f))
				
				Row {
					Text("개발자: ")
					Text(
						"lhwdev",
						style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
						modifier = Modifier.clickable {
							context.openWebsite("https://github.com/lhwdev")
						}
					)
				}
				
				Text(
					"공식 웹사이트",
					style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
					modifier = Modifier.clickable {
						context.openWebsite("https://github.com/lhwdev/covid-selftest-macro")
					}
				)
				
				Spacer(Modifier.weight(11f))
			}
		}
	}
}
