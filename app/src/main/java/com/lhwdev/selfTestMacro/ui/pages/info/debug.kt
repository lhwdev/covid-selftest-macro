package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lhwdev.github.repo.Repository
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.debug.dumpDebug
import com.lhwdev.selfTestMacro.debug.sIncludeLogcatInLog
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestSchedulesImpl
import com.lhwdev.selfTestMacro.repository.sDebugScheduleEnabled
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.pages.intro.IntroRoute
import com.vanpra.composematerialdialogs.*
import kotlinx.serialization.json.Json


fun Navigator.showDebugWindow() = showDialogAsync(maxHeight = Dp.Infinity) {
	val pref = LocalPreference.current
	val navigator = LocalNavigator
	val scope = rememberCoroutineScope()
	val selfTestManager = LocalSelfTestManager.current
	
	Title { Text("개발자 모드") }
	ListContent {
		ListItem(modifier = Modifier.clickable {
			pref.isDebugEnabled = false
		}) { Text("개발자 모드 끄기") }
		
		ListItem(modifier = Modifier.clickable {
			navigator.showDialogAsync {
				Title { Text("가상 서버 설정") }
				
				val (value, setValue) = remember {
					mutableStateOf(
						pref.virtualServer?.owner ?: Json.encodeToString(
							Repository.serializer(),
							App.github.repository
						)
					)
				}
				Input {
					TextField(value, setValue)
				}
				
				Buttons {
					PositiveButton(onClick = {
						try {
							pref.virtualServer = Json.decodeFromString(Repository.serializer(), value)
						} catch(th: Throwable) {
							navigator.showDialogAsync { Text("틀렸스비다\n" + th.stackTraceToString()) }
						}
					})
					
					NegativeButton(onClick = requestClose)
					Button(onClick = {
						pref.virtualServer = null
						requestClose()
					}) { Text("초기화") }
				}
			}
		}) { Text("가상 서버 ${if(pref.virtualServer != null) "설정" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isDebugCheckEnabled = !pref.isDebugCheckEnabled
		}) { Text("체크 ${if(pref.isDebugCheckEnabled) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			val new = pref.includeLogcatInLog
			pref.includeLogcatInLog = !new
			sIncludeLogcatInLog = new
		}) { Text("디버그 정보에 로그캣 포함 ${if(pref.includeLogcatInLog) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			navigator.pushRoute(IntroRoute)
		}) { Text("인트로 보기") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isNavigationDebugEnabled = !pref.isNavigationDebugEnabled
		}) { Text("AnimateListAsComposable & navigation 디버깅 ${if(pref.isNavigationDebugEnabled) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			pref.isScheduleDebugEnabled = !pref.isScheduleDebugEnabled
			sDebugScheduleEnabled = pref.isScheduleDebugEnabled
		}) { Text("SelfTestSchedule 등 예약 디버깅 ${if(pref.isScheduleDebugEnabled) "끄기" else "켜기"}") }
		
		ListItem(modifier = Modifier.clickable {
			println((selfTestManager.schedules as SelfTestSchedulesImpl).dumpDebug(oneLine = false))
		}) { Text("SelfTestSchedule 스케줄 덤프") }
		
		ListItem(modifier = Modifier.clickable {
			selfTestManager.schedules.updateTasks()
		}) { Text("SelfTestSchedule 스케줄 재생성") }
		
		ListItem(modifier = Modifier.clickable {
			(selfTestManager.schedules as SelfTestSchedulesImpl).recreateTasks()
		}) { Text("SelfTestSchedule 스케줄 강제 재생성") }
		
		ListItem { Text("에러 로깅 활성화됨") }
	}
}
