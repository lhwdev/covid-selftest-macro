package com.lhwdev.selfTestMacro.ui.pages.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.getText
import com.lhwdev.selfTestMacro.navigation.CustomDialogRouteTransition
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.onError
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.ScrimNavLightColor
import com.lhwdev.selfTestMacro.ui.TopAppBar
import com.zachklipp.richtext.markdown.Markdown
import com.zachklipp.richtext.ui.material.MaterialRichText


const val sPrivacyPolicyRaw = "https://api.github.com/repos/lhwdev/covid-selftest-macro/contents/PRIVACY_POLICY.md"
const val sPrivacyPolicyLink = "https://github.com/lhwdev/covid-selftest-macro/blob/master/PRIVACY_POLICY.md"


fun Navigator.showPrivacyPolicy() {
	pushRoute(transition = CustomDialogRouteTransition()) {
		AutoSystemUi(
			navigationBarMode = OnScreenSystemUiMode.Immersive(ScrimNavLightColor)
		) { scrims ->
			Scaffold(
				topBar = { TopAppBar(title = { Text("개인정보 처리 방침") }, statusBarScrim = scrims.statusBar) },
				backgroundColor = MaterialTheme.colors.surface
			) {
				val content = produceState<String?>(null) {
					value = try {
						fetch(
							sPrivacyPolicyRaw,
							headers = mapOf("Accept" to "application/vnd.github.v3.raw")
						).getText()
					} catch(th: Throwable) {
						onError(th, "개인정보 처리 방침을 불러오지 못했어요.")
						"""
							개인정보 처리 방침을 불러오지 못했어요. 네트워크에 연결되어 있는지 확인해주세요.
							만약 이 상태가 지속된다면 [여기를 눌러주세요.]($sPrivacyPolicyLink)
						""".trimIndent()
					}
				}.value
				
				Box {
					if(content != null) Column(Modifier.verticalScroll(rememberScrollState()).fillMaxSize()) {
						MaterialRichText(modifier = Modifier.padding(16.dp)) {
							Markdown(content)
						}
						
						scrims.navigationBarSpacer()
					}
					Box(Modifier.align(Alignment.BottomStart)) { scrims.navigationBar() }
				}
			}
		}
	}
}
