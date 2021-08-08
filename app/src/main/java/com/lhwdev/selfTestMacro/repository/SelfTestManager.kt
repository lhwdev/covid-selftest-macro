package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.DatabaseManager
import com.lhwdev.selfTestMacro.DbTestGroup
import com.lhwdev.selfTestMacro.DbTestSchedule


interface SelfTestManager {
	fun updateSchedule(target: DbTestGroup, schedule: DbTestSchedule)
	fun onScheduleUpdated(database: DatabaseManager)
}
