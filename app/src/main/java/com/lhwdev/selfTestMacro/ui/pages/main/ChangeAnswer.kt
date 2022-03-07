package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.utils.SelectionChip
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
				statusBarScrim = scrims.statusBar
			)
		}
	) {
		val (answer, setAnswer) = remember { mutableStateOf(user.answer) }
		
		Column {
			Column(Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
				ListItem(icon = { Icon(painterResource(iconFor(user)), contentDescription = null) }) {
					Text("${user.name} (${user.institute.name})")
				}
				
				SelectAnswerContent(answer, setAnswer)
			}
			
			Buttons {
				PositiveButton(onClick = {
					val users = pref.db.users
					pref.db.users = users.copy(
						users = users.users.replacedValue(user, user.copy(answer = answer))
					)
					
					requestClose()
				})
				
				NegativeButton(onClick = requestClose)
			}
			
			scrims.navigationBar()
		}
	}
}


@Composable
fun (@Suppress("unused") ColumnScope).SelectAnswerContent(answer: Answer, setAnswer: (Answer) -> Unit) {
	@Composable
	fun <T> Item(question: SelfTestQuestions<T>) {
		ListItem(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
			Column {
				Text(question.content)
				Spacer(Modifier.height(8.dp))
				Row(Modifier.padding(start = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					for((item, text) in question.displayTexts) {
						SelectionChip(
							selected = answer[question] == item,
							setSelected = { if(it) setAnswer(answer.with(question, item)) },
							trailingIconSelected = {
								Icon(painterResource(R.drawable.ic_check_24), contentDescription = null)
							}
						) { Text(text) }
					}
				}
			}
		}
	}
	
	Spacer(Modifier.height(8.dp))
	
	for(question in SelfTestQuestions.all) {
		Item(question)
	}
}
