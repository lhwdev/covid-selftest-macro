package com.lhwdev.selfTestMacro.repository

import android.content.Context
import com.lhwdev.selfTestMacro.DatabaseManager


interface SelfTestScheduler {
	fun onScheduleUpdated(database: DatabaseManager, context: Context)
}
