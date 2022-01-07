package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.pages.intro.IntroRoute
import com.vanpra.composematerialdialogs.ListContent
import com.vanpra.composematerialdialogs.Title
import com.vanpra.composematerialdialogs.showDialogAsync


fun Navigator.showDebugWindow() = showDialogAsync {
	val pref = LocalPreference.current
	val navigator = LocalNavigator
	
	Title { Text("개발자 모드") }
	ListContent {
		ListItem(modifier = Modifier.clickable {
			pref.isDebugEnabled = false
		}) { Text("개발자 모드 끄기") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isVirtualServer = !pref.isVirtualServer
		}) { Text("가상 서버 ${if(pref.isVirtualServer) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isDebugFetchEnabled = !pref.isDebugFetchEnabled
		}) { Text("fetch 디버깅 ${if(pref.isDebugFetchEnabled) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			navigator.pushRoute(IntroRoute)
		}) { Text("인트로 보기") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isNavigationDebugEnabled = !pref.isNavigationDebugEnabled
		}) { Text("AnimateListAsComposable & navigation 디버깅 ${if(pref.isNavigationDebugEnabled) "끄기" else "켜기"}") }
		
		ListItem { Text("에러 로깅 활성화됨") }
	}
}
