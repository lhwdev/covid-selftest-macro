package com.lhwdev.selfTestMacro.ui.pages.intro

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.lhwdev.fetch.get
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.ScrimNavLightColor
import com.lhwdev.selfTestMacro.ui.TopAppBar
import com.vanpra.composematerialdialogs.showFullDialogAsync

private const val sPrivacyPolicyName = "PRIVACY_POLICY.md"
// private const val sPrivacyPolicyLink = "https://github.com/lhwdev/covid-selftest-macro/blob/master/PRIVACY_POLICY.md"


fun Navigator.showPrivacyPolicy(): Unit = showFullDialogAsync {
	AutoSystemUi(
		navigationBarMode = OnScreenSystemUiMode.Immersive(ScrimNavLightColor)
	) { scrims ->
		Scaffold(
			topBar = { TopAppBar(title = { Text("개인정보 처리 방침") }, statusBarScrim = scrims.statusBar) }
		) {
			Box {
				Column(Modifier.verticalScroll(rememberScrollState()).fillMaxSize()) {
					AndroidView(
						factory = {
							WebView(it).apply {
								loadUrl(App.githubRepo.url["contents/$sPrivacyPolicyName"].toString())
							}
						}
					)
					
					scrims.navigationBarSpacer()
				}
				Box(Modifier.align(Alignment.BottomStart)) { scrims.navigationBar() }
			}
		}
	}
}
