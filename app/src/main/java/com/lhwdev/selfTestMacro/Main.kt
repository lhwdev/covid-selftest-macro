package com.lhwdev.selfTestMacro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.lhwdev.selfTestMacro.api.getUserGroup
import com.lhwdev.selfTestMacro.api.getUserInfo


@Composable
fun Main() {
	val context = AmbientActivity.current
	val pref = AmbientPreference.current
	val scope = rememberCoroutineScope()
	
	if(pref.firstState == 0) {
		AmbientRoute.current[0] = { Setup() }
		return
	}
	
	pref.firstState = 1
	LaunchedEffect(null) { context.checkNotice() }
	
	val userInfo by lazyState {
		val institute = pref.institute!!
		val loginInfo = pref.user!!
		
		try {
			val user = context.singleOfUserGroup(getUserGroup(institute, loginInfo.token))
			if(user == null) null else getUserInfo(institute, user)
		} catch(e: Throwable) {
			context.onError(e, "사용자 정보 불러오기")
			context.showToastSuspendAsync("사용자 정보를 불러오지 못했습니다.")
			null
		}
	}
	
	Scaffold(
		topBar = {
			TopAppBar(title = { Text("코로나19 자가진단 매크르") })
		}
	) { modifier ->
		Column(Modifier.padding(modifier)) {
			val user = userInfo
			
			Text(
				if(user == null) "사용자 정보 불러오는 중..." else
					"${user.toUserInfoString()}\n${user.toLastRegisterInfoString()}",
				style = MaterialTheme.typography.h3,
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
			)
			
			val (checked, updateChecked) = remember { mutableStateOf(false) }
			TextSwitch(
				checked, updateChecked,
				text = { Text("자가진단") },
				switch = { Switch(checked, updateChecked) }
			)
		}
	}
}

