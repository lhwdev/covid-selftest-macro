package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.AlarmReceiver
import com.lhwdev.selfTestMacro.DatabaseManager


private fun Context.createIntent(id: Int): PendingIntent = PendingIntent.getBroadcast(
	this,
	id,
	Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
		PendingIntent.FLAG_IMMUTABLE
	} else {
		0
	}
)


class SelfTestSchedulerImpl(val context: Context, database: DatabaseManager) : SelfTestScheduler {
	private val lastGroups = database.testGroups.groups
	private val intentCache = mutableMapOf<Int, PendingIntent>()
	
	private fun intentCache(id: Int) = intentCache.getOrPut(id) {
		context.createIntent(id)
	}
	
	
	override fun onScheduleUpdated(database: DatabaseManager) {
		val newGroups = database.testGroups.groups
		if(lastGroups == newGroups) return
		
		val added = newGroups - lastGroups
		val removed = lastGroups - newGroups
		
		val alarmManager = context.getSystemService<AlarmManager>()!!
		for(group in removed) {
			val intent = intentCache(id = group.id)
			alarmManager.cancel(intent)
		}
		
		for(group in added) {
			val intent = intentCache(id = group.id)
			alarmManager.setExact()
		}
	}
}
