package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.ui.SelfTestQuestions
import com.vanpra.composematerialdialogs.*


@Composable
fun FloatingMaterialDialogScope.OneUserDetail(user: DbUser, status: Status) {
	val navigator = LocalNavigator
	
	Title { Text("${user.name}의 자가진단 현황") }
	
	Content(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		val text = when(status) {
			is Status.NotSubmitted -> "자가진단 제출 안함"
			is Status.Submitted -> status.suspicious?.displayName ?: "정상"
		}
		ListItem { Text(text) }
		
		
		if(status is Status.Submitted) {
			ListItem {
				Text("'${SelfTestQuestions.suspicious}': ${if(status.questionSuspicious == true) "있음" else "없음"}")
			}
			ListItem {
				Text("'${SelfTestQuestions.waitingResult}': ${if(status.questionWaitingResult == true) "있음" else "없음"}")
			}
			ListItem {
				Text("'${SelfTestQuestions.quarantined}': ${if(status.questionQuarantined == true) "있음" else "없음"}")
			}
			ListItem {
				Text("'${SelfTestQuestions.housemateInfected}': ${if(status.questionQuarantined == true) "있음" else "없음"}")
			}
		}
	}
	
	Buttons {
		Button(onClick = {
			navigator.showChangeAnswerDialog(user)
		}) { Text("응답 바꾸기") }
		PositiveButton(onClick = requestClose) { Text("확인") }
	}
}
