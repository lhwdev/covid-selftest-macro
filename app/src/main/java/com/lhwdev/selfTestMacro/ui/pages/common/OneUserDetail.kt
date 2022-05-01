package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.ErrorInfo
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestSchedulesImpl
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.SelfTestQuestions
import com.lhwdev.selfTestMacro.ui.UiContext
import com.lhwdev.selfTestMacro.ui.pages.main.promptSelectAnswerDialog
import com.lhwdev.selfTestMacro.ui.pages.main.showChangeAnswerDialog
import com.lhwdev.selfTestMacro.ui.primaryContainer
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch


@Composable
fun FloatingMaterialDialogScope.OneUserDetail(
	group: DbTestGroup,
	user: DbUser,
	status: Status?,
	statusKey: MutableState<Int>
) {
	val navigator = LocalNavigator
	val selfTestManager = LocalSelfTestManager.current
	val context = LocalContext.current
	
	Title { Text("${user.name}의 자가진단 현황") }
	
	Content(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		val text = when(status) {
			null -> "로딩 중"
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
				val scope = navigator.parentOrSelf.coroutineScope
				val uiContext = UiContext(
					context = context,
					navigator = navigator,
					showMessage = { message, _ ->
						context.showToastSuspendAsync(message)
					},
					scope = scope
				)
				scope.launch {
					val answer = navigator.promptSelectAnswerDialog(
						title = "자가진단 제출하기",
						user = user,
						positiveText = "제출"
					) ?: return@launch
					
					submitNowInProgress = true
					// > `Note: users may not be derived from database, rather arbitrary modified data to change answer etc.`
					selfTestManager.submitSelfTestNow(
						uiContext,
						group = group,
						users = listOf(user.copy(answer = answer))
					)
					submitNowInProgress = false
					statusKey.value++
				}
			}
		) { Text(if(submitNowInProgress) "제출하는 중" else "지금 자가진단 제출") }
		
		if(LocalPreference.current.isDebugEnabled) {
			val scope = rememberCoroutineScope()
			TextButton(onClick = {
				val schedules = selfTestManager.schedules as SelfTestSchedulesImpl
				val task = schedules.getTask(group, user) ?: schedules.getTasks(group).find { it.userId == null }
				
				scope.launch {
					if(task == null) {
						selfTestManager.debugContext.showErrorInfo(
							ErrorInfo(message = "task가 없어요!!", severity = DebugContext.Severity.light),
							description = "저런"
						)
					} else {
						schedules.scheduler.onTask(task)
					}
				}
			}) { Text("(디버깅) 예약실행 시뮬레이션") }
		}
	}
	
	Buttons {
		Button(onClick = {
			navigator.showChangeAnswerDialog(user)
		}) { Text("응답 바꾸기") }
		PositiveButton(onClick = requestClose)
	}
}
