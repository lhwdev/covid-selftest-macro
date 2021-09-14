package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.repository.Status
import com.vanpra.composematerialdialogs.FloatingMaterialDialogScope
import com.vanpra.composematerialdialogs.ListContent
import com.vanpra.composematerialdialogs.Title


@Composable
fun FloatingMaterialDialogScope.OneUserDetail(user: DbUser, status: Status) {
	Title { Text("${user.name}의 자가진단 현황") }
	
	ListContent {
		Column {
			val text = when(status) {
				is Status.NotSubmitted -> "자가진단 제출 안함"
				is Status.Submitted -> status.suspicious?.displayName ?: "정상"
			}
			ListItem { Text(text) }
			
			if(status is Status.Submitted) {
				ListItem {
					Text("'1. 학생 본인이 코로나19가 의심되는 임상증상이 있나요?': ${if(status.questionSuspicious == true) "있음" else "없음"}")
				}
				ListItem {
					Text("'2. 학생 본인 또는 동거인이 코로나19 진단검사를 받고 그 결과를 기다리고 있나요?': ${if(status.questionWaitingResult == true) "있음" else "없음"}")
				}
				ListItem {
					Text("'3. 학생 본인 또는 동거인이 방역당국에 의해 현재 자가격리가 이루어지고 있나요?': ${if(status.questionQuarantined == true) "있음" else "없음"}")
				}
			}
		}
	}
}
