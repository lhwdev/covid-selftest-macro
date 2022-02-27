package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.QuickTestResult
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
		val (a2, setA2) = remember { mutableStateOf(user.answer.quickTestResult) }
		val (a3, setA3) = remember { mutableStateOf(user.answer.waitingResult) }
		val (a4, setA4) = remember { mutableStateOf(user.answer.quarantined) }
		val (a5, setA5) = remember { mutableStateOf(user.answer.housemateInfected) }
		
		Column {
			Divider()
			
			ListItem(icon = { Icon(painterResource(iconFor(user)), contentDescription = null) }) {
				Text("${user.name} (${user.institute.name})")
			}
			
			val modifier = Modifier.padding(vertical = 4.dp)
			
			CheckBoxListItem(checked = a1, onCheckChanged = setA1, modifier = modifier) {
				Text(SelfTestQuestions.suspicious)
			}
			ListItem(icon = {}) {
				Text(SelfTestQuestions.quickTestResult)
			}
			Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
				@Composable
				fun Item(enum: QuickTestResult) {
					CheckBoxListItem(a2 == enum, { setA2(enum) }) {
						Text(enum.displayLabel)
					}
				}
				
				Item(QuickTestResult.didNotConduct)
				Item(QuickTestResult.negative)
				Item(QuickTestResult.positive)
			}
			CheckBoxListItem(checked = a3, onCheckChanged = setA3, modifier = modifier) {
				Text(SelfTestQuestions.waitingResult)
			}
			CheckBoxListItem(checked = a4, onCheckChanged = setA4, modifier = modifier) {
				Text(SelfTestQuestions.quarantined)
			}
			CheckBoxListItem(checked = a5, onCheckChanged = setA5, modifier = modifier) {
				Text(SelfTestQuestions.housemateInfected)
			}
			
			Spacer(Modifier.weight(1f))
			
			Buttons {
				PositiveButton(onClick = {
					val users = pref.db.users
					val newAnswer = Answer(
						suspicious = a1,
						quickTestResult = a2,
						waitingResult = a3,
						quarantined = a4,
						housemateInfected = a5
					)
					
					pref.db.users = users.copy(
						users = users.users.replacedValue(user, user.copy(answer = newAnswer))
					)
				})
				
				NegativeButton(onClick = requestClose)
			}
			
			scrims.navigationBar()
		}
	}
}
