package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.SelfTestTask
import com.vanpra.composematerialdialogs.*


private fun SelfTestTask.timeToString() = timeMillis.millisToLocalizedString()

fun Navigator.showTodayScheduleDialog(testGroup: DbTestGroup) = showDialogAsync {
	Title { Text("오늘 일정") }
	
	Content {
		val selfTestManager = LocalSelfTestManager.current
		val tasks = remember(testGroup) { selfTestManager.schedules.getTasks(testGroup) }
		
		if(tasks.isEmpty()) {
			Text("예약이 설정되지 않았어요.")
			return@Content
		}
		
		if(testGroup.schedule.altogether) {
			val task = tasks.first()
			
			Text(task.timeToString())
		} else Column(Modifier.verticalScroll(rememberScrollState())) {
			for(task in tasks) {
				val name = selfTestManager.database.users.users[task.userId!!]?.name
				Text("$name: ${task.timeToString()}")
			}
		}
	}
	
	Buttons {
		PositiveButton(onClick = requestClose)
	}
}
