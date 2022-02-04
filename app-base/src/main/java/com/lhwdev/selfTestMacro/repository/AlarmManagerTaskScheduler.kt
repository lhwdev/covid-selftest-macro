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
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer



private const val sSetExactInterval = 18 * 60 * 1000 // 18m


private fun Context.defaultPreferenceHolder() = preferenceHolderOf("AlarmManagerTaskScheduler")


/**
 * A scheduler implementation that utilizes [AlarmManager].
 * As the limitation of it, this class extends from [GroupTaskScheduler] to remove the limit of tasks.
 *
 * All the schedules basically work in day-to-day basis, meaning if there is no left task to be run today, then before
 * moving to tomorrow, [updateTomorrow] is called. You need to implement proper logic that might call [updateTasks],
 * so that tasks for the next day are properly handled.
 */
abstract class AlarmManagerTaskScheduler<T : TaskItem>(
	initialTasks: List<T>,
	val context: Context,
	val scheduleIntent: Intent,
	val holder: PreferenceHolder = context.defaultPreferenceHolder()
) : GroupTaskScheduler<T>(initialTasks = initialTasks) {
	private val manager = context.getSystemService<AlarmManager>()!!
	
	
	protected open fun currentTimeMillis(): Long = System.currentTimeMillis()
	
	
	/// Tasks
	override var taskId: Long by holder.preferenceLong(
		key = "taskId",
		defaultValue = Long.MIN_VALUE
	)
	
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
	
	protected var alarmSchedule: TaskSchedule? by holder.preferenceSerialized(
		key = "alarmSchedule",
		serializer = TaskSchedule.serializer().nullable,
		defaultValue = null
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
		
		return TaskSchedule(code = code, timeMillis = time)
	}
	
	override fun scheduleCancel(schedule: TaskSchedule) {
		// if(schedule.timeMillis < System.currentTimeMillis()) return
	}
	
	override suspend fun onSchedule(schedule: TaskSchedule, coroutineScope: CoroutineScope) {
		val index = schedules.indexOf(schedule)
		val next = schedules.getOrNull(index + 1)
		
		val updateTomorrow = if(next == null) {
			true
		} else {
			dayOf(next.timeMillis) > dayOf(currentTimeMillis())
		}
		if(updateTomorrow) updateTomorrow()
		
		val newNext = schedules.getOrNull(index + 1) // maybe updated from next, from updateTomorrow
		if(newNext != null) {
			scheduleAlarm(newNext.code, timeMillis = newNext.timeMillis)
		}
		
		super.onSchedule(schedule, coroutineScope)
	}
	
	protected abstract fun updateTomorrow()
	
	override fun onScheduleUpdated() {
		val new = schedules.firstOrNull()
		val last = alarmSchedule
		
		if(new != last) {
			if(last != null) {
				cancelAlarm(last.code)
			}
			
			if(new != null) {
				scheduleAlarm(new.code, timeMillis = new.timeMillis)
			}
		}
	}
	
	private fun scheduleAlarm(code: Int, timeMillis: Long) {
		val intent = schedulerIntent(code = code)
		
		if(Build.VERSION.SDK_INT >= 23) {
			manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, intent)
		} else {
			manager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, intent)
		}
	}
	
	private fun cancelAlarm(code: Int) {
		val intent = schedulerIntent(code = code)
		manager.cancel(intent)
	}
	
	/// Etc
	
	open fun cleanOldSchedules() {
		val current = currentTimeMillis()
		
		val index = schedules.indexOfFirst { it.timeMillis > current }
		if(index > 0) schedules = schedules.drop(index)
	}
}
