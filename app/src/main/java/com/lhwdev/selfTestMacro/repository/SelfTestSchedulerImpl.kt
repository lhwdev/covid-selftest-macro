package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.AlarmReceiver
import com.lhwdev.selfTestMacro.DatabaseManager


private fun Context.createIntent(): PendingIntent = PendingIntent.getBroadcast(
	this, AlarmReceiver.REQUEST_CODE, Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT
)


class SelfTestSchedulerImpl(val context: Context, database: DatabaseManager) : SelfTestScheduler {
	private val lastGroups = database.testGroups.groups
	private val indentCache = mutableMapOf<Int, PendingIntent>()

	private fun indentCache(id: Int) = indentCache.getOrPut(id) {
		PendingIntent.getBroadcast(context)
	}


	override fun onScheduleUpdated(database: DatabaseManager) {
		val newGroups = database.testGroups.groups
		if (lastGroups == newGroups) return

		val added = newGroups - lastGroups
		val removed = lastGroups - newGroups

		val alarmManager = context.getSystemService<AlarmManager>()

	}
}
