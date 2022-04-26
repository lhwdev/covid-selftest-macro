package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.compose.material.Text
import androidx.compose.runtime.remember
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.utils.millisToDuration
import com.lhwdev.selfTestMacro.utils.toLocalizedString
import com.vanpra.composematerialdialogs.*


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
			
			Text("오늘 ${task.timeMillis.millisToDuration().toLocalizedString()}")
		}
		
		
	}
	
	Buttons {
		PositiveButton(onClick = requestClose)
	}
}
