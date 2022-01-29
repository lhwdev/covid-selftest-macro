package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.replacedValue
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.common.CheckBoxListItem
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.vanpra.composematerialdialogs.*


fun Navigator.showChangeAnswerDialog(user: DbUser) = showFullDialogAsync { dismiss ->
	ChangeAnswer(user, dismiss)
}

@Composable
fun MaterialDialogScope.ChangeAnswer(user: DbUser, dismiss: () -> Unit): Unit = AutoSystemUi(
	statusBarMode = OnScreenSystemUiMode.Immersive()
) { scrims ->
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
		val (a4, setA4) = remember { mutableStateOf(user.answer.housemateInfected) }
		
		Column {
			Divider()
			
			ListItem(icon = { Icon(painterResource(iconFor(user)), contentDescription = null) }) {
				Text("${user.name} (${user.institute.name})")
			}
			
			val modifier = Modifier.padding(vertical = 4.dp)
			
			CheckBoxListItem(checked = a1, onCheckChanged = setA1, modifier = modifier) {
				Text(SelfTestQuestions.suspicious)
			}
			CheckBoxListItem(checked = a2, onCheckChanged = setA2, modifier = modifier) {
				Text(SelfTestQuestions.waitingResult)
				Spacer(Modifier.height(8.dp))
			}
			CheckBoxListItem(checked = a3, onCheckChanged = setA3, modifier = modifier) {
				Text(SelfTestQuestions.quarantined)
			}
			CheckBoxListItem(checked = a4, onCheckChanged = setA4, modifier = modifier) {
				Text(SelfTestQuestions.housemateInfected)
			}
			
			Spacer(Modifier.weight(1f))
			
			Buttons {
				PositiveButton(onClick = {
					val users = pref.db.users
					val newAnswer = Answer(
						suspicious = a1,
						waitingResult = a2,
						quarantined = a3,
						housemateInfected = a4
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
