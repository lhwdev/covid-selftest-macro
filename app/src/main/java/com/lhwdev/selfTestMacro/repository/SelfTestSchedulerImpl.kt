package com.lhwdev.selfTestMacro.repository

import android.content.Context
import com.lhwdev.selfTestMacro.DatabaseManager


class SelfTestSchedulerImpl(database: DatabaseManager) : SelfTestScheduler {
	private val lastGroups = database.testGroups.groups
	
	override fun onScheduleUpdated(database: DatabaseManager, context: Context) {
		val newGroups = database.testGroups.groups
		if(lastGroups == newGroups) return
		
		val added = newGroups - lastGroups
		val removed = lastGroups - newGroups
		
	}
}
