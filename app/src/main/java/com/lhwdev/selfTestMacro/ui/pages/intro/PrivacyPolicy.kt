package com.lhwdev.selfTestMacro.ui.pages.intro

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lhwdev.fetch.getText
import com.lhwdev.github.repo.GithubContentType
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.ui.*
import com.vanpra.composematerialdialogs.showFullDialogAsync


private const val sPrivacyPolicyName = "PRIVACY_POLICY.md"
private const val sPrivacyPolicyLink = "https://github.com/lhwdev/covid-selftest-macro/blob/master/PRIVACY_POLICY.md"


fun Navigator.showPrivacyPolicy(): Unit = showFullDialogAsync {
	AutoSystemUi(
		navigationBarMode = OnScreenSystemUiMode.Immersive(ScrimNavSurfaceColor)
	) { scrims ->
		val uriHandler = LocalUriHandler.current
		
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("개인정보 처리 방침") },
					actions = {
						IconButton(onClick = { uriHandler.openUri(sPrivacyPolicyLink) }) {
							Icon(painterResource(R.drawable.ic_open_in_browser_24), contentDescription = "브라우저에서 열기")
						}
					},
					statusBarScrim = scrims.statusBar
				)
			}
		) {
			val data = produceState<String?>(null) {
				value = App.masterBranch.getContent(
					sPrivacyPolicyName,
					accept = GithubContentType.html
				).getText()
			}.value
			
			Box(Modifier.fillMaxSize()) {
				if(data != null) Column(Modifier.verticalScroll(rememberScrollState()).fillMaxSize()) {
					val foreground = DefaultContentColor
					
					AndroidView(
						factory = {
							WebView(it).apply {
								setBackgroundColor(0x00000000)
								
								// eew, dirty
								loadDataWithBaseURL(
									sPrivacyPolicyLink,
									"""
										<html>
										<head>
											<style>
												* {
													color: #${
										(foreground.toArgb() and 0xffffff).toString(radix = 16).padStart(6, '0')
									}ff;
												}
											</style>
										</head>
										<body>$data</body>
										</html>
									""",
									"text/html",
									"utf-8",
									sPrivacyPolicyLink
								)
							}
						},
						modifier = Modifier.padding(top = 32.dp, start = 12.dp, end = 12.dp, bottom = 24.dp)
					)
					
					scrims.navigationBarSpacer()
				} else {
					Text("불러오는 중...", modifier = Modifier.padding(32.dp).align(Alignment.TopCenter))
				}
				Box(Modifier.align(Alignment.BottomStart)) { scrims.navigationBar() }
			}
		}
	}
}
