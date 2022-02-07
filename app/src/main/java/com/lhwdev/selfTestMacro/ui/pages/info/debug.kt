package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.pages.intro.IntroRoute
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch


fun Navigator.showDebugWindow() = showDialogAsync {
	val pref = LocalPreference.current
	val navigator = LocalNavigator
	val scope = rememberCoroutineScope()
	
	Title { Text("개발자 모드") }
	ListContent {
		ListItem(modifier = Modifier.clickable {
			pref.isDebugEnabled = false
		}) { Text("개발자 모드 끄기") }
		
		ListItem(modifier = Modifier.clickable {
			scope.launch {
				val info = navigator.showDialog<String?> {
					Title { Text("가상 서버 설정") }
					
					val (url, setUrl) = remember { mutableStateOf(pref.virtualServer ?: "") }
					Input {
						TextField(url, setUrl, label = { Text("URL") })
					}
					
					Buttons {
						PositiveButton(onClick = {
							pref.virtualServer = url.ifEmpty { null }
						})
						
						NegativeButton(onClick = requestClose)
					}
				}
			}
		}) { Text("가상 서버 ${if(pref.virtualServer != null) "설정" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isDebugCheckEnabled = !pref.isDebugCheckEnabled
		}) { Text("체크 ${if(pref.isDebugFetchEnabled) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			navigator.pushRoute(IntroRoute)
		}) { Text("인트로 보기") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isNavigationDebugEnabled = !pref.isNavigationDebugEnabled
		}) { Text("AnimateListAsComposable & navigation 디버깅 ${if(pref.isNavigationDebugEnabled) "끄기" else "켜기"}") }
		
		ListItem { Text("에러 로깅 활성화됨") }
	}
}
