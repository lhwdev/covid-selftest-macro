package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.database.*
import kotlinx.serialization.builtins.ListSerializer



private const val sSetExactInterval = 18 * 60 * 1000 // 18m


private fun Context.defaultPreferenceHolder() = preferenceHolderOf("AlarmManagerTaskScheduler")


abstract class AlarmManagerTaskScheduler<T : TaskItem>(
	val context: Context,
	val scheduleIntent: Intent,
	val holder: PreferenceHolder = context.defaultPreferenceHolder()
) : GroupTaskScheduler<T>() {
	private val manager = context.getSystemService<AlarmManager>()!!
	
	
	/// Tasks
	override var taskId: Long by holder.preferenceLong(
		key = "taskId",
		defaultValue = Long.MIN_VALUE
	)
	
	abstract override var tasks: List<T>
	
	override fun canTaskScheduled(task: T, schedule: TaskSchedule): Boolean =
		task.timeMillis - schedule.timeMillis in 0..sSetExactInterval
	
	
	/// Schedules
	override var scheduleId: Int by holder.preferenceInt(
		key = "scheduleId",
		defaultValue = Int.MIN_VALUE
	)
	
	override var schedules: List<TaskSchedule> by holder.preferenceSerialized(
		key = "schedules",
		serializer = ListSerializer(TaskSchedule.serializer()),
		defaultValue = emptyList()
	)
	
	private val schedulerFlags = PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
		PendingIntent.FLAG_IMMUTABLE
	} else {
		0
	}
	
	// This function should be idempotent, so that it can be used to AlarmManager.cancel
	private fun schedulerIntent(code: Int): PendingIntent =
		PendingIntent.getBroadcast(context, code, scheduleIntent, schedulerFlags)
	
	override fun scheduleSet(time: Long): TaskSchedule {
		val code = nextScheduleId()
		val intent = schedulerIntent(code = code)
		
		if(Build.VERSION.SDK_INT >= 23) {
			manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
		} else {
			manager.setExact(AlarmManager.RTC_WAKEUP, time, intent)
		}
		return TaskSchedule(code = code, timeMillis = time)
	}
	
	override fun scheduleCancel(schedule: TaskSchedule) {
		if(schedule.timeMillis < System.currentTimeMillis()) return
		
		val intent = schedulerIntent(code = schedule.code)
		manager.cancel(intent)
	}
	
	
	/// Etc
	
	open fun cleanOldSchedules() {
		val current = System.currentTimeMillis()
		
		val index = schedules.indexOfFirst { it.timeMillis > current }
		if(index > 0) schedules = schedules.drop(index)
	}
}
