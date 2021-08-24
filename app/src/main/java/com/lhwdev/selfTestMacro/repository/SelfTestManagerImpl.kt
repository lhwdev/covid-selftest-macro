package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.AlarmReceiver
import com.lhwdev.selfTestMacro.api.SurveyData
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.replaced
import java.util.Calendar
import kotlin.random.Random
import kotlin.random.nextInt


private fun Context.createScheduleIntent(id: Int): PendingIntent = PendingIntent.getBroadcast(
	this,
	id,
	Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
		PendingIntent.FLAG_IMMUTABLE
	} else {
		0
	}
)


class SelfTestManagerImpl(private val context: Context, private val database: DatabaseManager) : SelfTestManager {
	override suspend fun sessionFor(group: DbUserGroup): Session {
	}
	
	override suspend fun getCurrentStatus(user: DbUser): Status? {
	}
	
	override suspend fun Context.submitSelfTestNow(
		manager: DatabaseManager,
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult> {
	}
	
	/// Scheduling
	
	private val random = Random(System.currentTimeMillis())
	private val lastGroups = database.testGroups.groups
	private val intentCache = mutableMapOf<Int, PendingIntent>()
	
	private fun intentCache(id: Int) = intentCache.getOrPut(id) {
		context.createScheduleIntent(id)
	}
	
	
	private fun DbTestGroup.nextTime(): Long {
		val resultHour: Int
		val resultMinute: Int
		when(val schedule = schedule) {
			is DbTestSchedule.Fixed -> {
				resultHour = schedule.hour
				resultMinute = schedule.minute
			}
			is DbTestSchedule.Random -> {
				resultHour = random.nextInt(schedule.from.hour..schedule.to.hour)
				resultMinute = random.nextInt(schedule.from.minute..schedule.to.minute)
			}
			DbTestSchedule.None -> return -1L
		}
		
		val calendar = Calendar.getInstance()
		calendar[Calendar.SECOND] = 0
		calendar[Calendar.MILLISECOND] = 0
		
		calendar[Calendar.HOUR_OF_DAY] = resultHour
		calendar[Calendar.MINUTE] = resultMinute
		while(true) {
			if(excludeWeekend) {
				val day = calendar[Calendar.DAY_OF_WEEK]
				if(day == Calendar.SATURDAY || day == Calendar.SUNDAY) continue
			}
			
			break
		}
		
		
		return calendar.timeInMillis
	}
	
	private fun setSchedule(alarmManager: AlarmManager, target: DbTestGroup) {
		val time = target.nextTime()
		
		if(time != -1L) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, intentCache(id = target.id))
		}
	}
	
	override fun updateSchedule(target: DbTestGroup, schedule: DbTestSchedule) {
		val testGroups = database.testGroups
		
		val alarmManager = context.getSystemService<AlarmManager>()!!
		alarmManager.cancel(intentCache(target.id))
		
		val newTarget = target.copy(schedule = schedule)
		
		// change testGroups -> preferenceState.cache updated -> state update -> snapshot mutation -> snapshotFlow(see ComposeApp.kt) -> call onScheduleUpdated
		disableOnScheduleUpdated = true
		database.testGroups = testGroups.copy(groups = testGroups.groups.replaced(from = target, to = newTarget))
		disableOnScheduleUpdated = false
		
		setSchedule(alarmManager, newTarget)
	}
	
	private var disableOnScheduleUpdated: Boolean = false
	
	override fun onScheduleUpdated(database: DatabaseManager) {
		if(disableOnScheduleUpdated) return
		
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
			setSchedule(alarmManager, group)
		}
	}
}
