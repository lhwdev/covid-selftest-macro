package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.Answer
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.replacedValue
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.SelfTestQuestions
import com.lhwdev.selfTestMacro.ui.TopAppBar
import com.lhwdev.selfTestMacro.ui.common.CheckBoxListItem
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.vanpra.composematerialdialogs.*


@Composable
fun FloatingMaterialDialogScope.OneUserDetail(user: DbUser, status: Status) {
	val navigator = LocalNavigator
	
	Title { Text("${user.name}의 자가진단 현황") }
	
	ListContent {
		Column {
			val text = when(status) {
				is Status.NotSubmitted -> "자가진단 제출 안함"
				is Status.Submitted -> status.suspicious?.displayName ?: "정상"
			}
			ListItem { Text(text) }
			
			Spacer(Modifier.height(8.dp))
			
			if(status is Status.Submitted) {
				ListItem {
					Text("'1. ${SelfTestQuestions.suspicious}': ${if(status.questionSuspicious == true) "있음" else "없음"}")
				}
				ListItem {
					Text("'2. ${SelfTestQuestions.waitingResult}': ${if(status.questionWaitingResult == true) "있음" else "없음"}")
				}
				ListItem {
					Text("'3. ${SelfTestQuestions.quarantined}': ${if(status.questionQuarantined == true) "있음" else "없음"}")
				}
			}
		}
	}
	
	Buttons {
		Button(onClick = {
			navigator.showFullDialogAsync { dismiss ->
				ChangeAnswer(user, dismiss)
			}
		}) { Text("응답 바꾸기") }
		PositiveButton(onClick = requestClose) { Text("확인") }
	}
}


@Composable
fun FullMaterialDialogScope.ChangeAnswer(user: DbUser, dismiss: () -> Unit): Unit = AutoSystemUi { scrims ->
	val pref = LocalPreference.current
	
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("응답 바꾸기") },
				navigationIcon = {
					SimpleIconButton(icon = R.drawable.ic_clear_24, contentDescription = "닫기", onClick = dismiss)
				},
				statusBarScrim = scrims.statusBar,
				elevation = 0.dp
			)
		}
	) {
		
		
		val (a1, setA1) = remember { mutableStateOf(user.answer.suspicious) }
		val (a2, setA2) = remember { mutableStateOf(user.answer.waitingResult) }
		val (a3, setA3) = remember { mutableStateOf(user.answer.quarantined) }
		
		Column {
			Divider()
			
			ListItem(icon = { Icon(painterResource(iconFor(user)), contentDescription = null) }) {
				Text("${user.name} (${user.institute.name})")
			}
			
			CheckBoxListItem(checked = a1, onCheckChanged = setA1) { Text(SelfTestQuestions.suspicious) }
			CheckBoxListItem(checked = a2, onCheckChanged = setA2) { Text(SelfTestQuestions.waitingResult) }
			CheckBoxListItem(checked = a3, onCheckChanged = setA3) { Text(SelfTestQuestions.quarantined) }
			
			Spacer(Modifier.weight(1f))
			
			this@ChangeAnswer.Buttons {
				PositiveButton(onClick = {
					val users = pref.db.users
					val newAnswer = Answer(
						suspicious = a1,
						waitingResult = a2,
						quarantined = a3
					)
					
					pref.db.users = users.copy(
						users = users.users.replacedValue(user, user.copy(answer = newAnswer))
					)
				}) { Text("확인") }
				
				NegativeButton(onClick = requestClose) { Text("취소") }
			}
			
			scrims.navigationBar()
		}
	}
}
