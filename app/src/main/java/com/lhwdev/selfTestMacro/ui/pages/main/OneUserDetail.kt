package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import com.lhwdev.selfTestMacro.ui.SelfTestQuestions
import com.lhwdev.selfTestMacro.ui.UiContext
import com.lhwdev.selfTestMacro.ui.primaryContainer
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch


@Composable
fun FloatingMaterialDialogScope.OneUserDetail(
	group: DbTestGroup,
	user: DbUser,
	status: Status,
	statusKey: MutableState<Int>
) {
	val navigator = LocalNavigator
	val selfTestManager = LocalSelfTestManager.current
	val context = LocalContext.current
	
	Title { Text("${user.name}의 자가진단 현황") }
	
	Content(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		val text = when(status) {
			is Status.NotSubmitted -> "자가진단 제출 안함"
			is Status.Submitted -> (status.suspicious?.displayText ?: "정상") + " (${status.time})"
		}
		ListItem { Text(text) }
		
		
		if(status is Status.Submitted) for(question in SelfTestQuestions.all) {
			ListItem {
				@Suppress("UNCHECKED_CAST") (question as SelfTestQuestions<Any>)
				
				Text("'${question.title}': ${question.displayText(status.answer[question])}")
			}
		}
		
		Spacer(Modifier.height(8.dp))
		
		var submitNowInProgress by remember { mutableStateOf(false) }
		
		RoundButton(
			enabled = !submitNowInProgress,
			colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryContainer),
			onClick = {
				val scope = navigator.parent!!.coroutineScope
				val uiContext = UiContext(
					context = context,
					navigator = navigator,
					showMessage = { message, _ ->
						context.showToastSuspendAsync(message)
					},
					scope = scope
				)
				scope.launch {
					submitNowInProgress = true
					selfTestManager.submitSelfTestNow(uiContext, group = group, users = listOf(user))
					submitNowInProgress = false
					statusKey.value++
				}
			}
		) { Text(if(submitNowInProgress) "제출하는 중" else "지금 자가진단 제출") }
	}
	
	Buttons {
		Button(onClick = {
			navigator.showChangeAnswerDialog(user)
		}) { Text("응답 바꾸기") }
		PositiveButton(onClick = requestClose)
	}
}
