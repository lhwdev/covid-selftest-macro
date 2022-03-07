package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.debug.debugFlow
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
 * moving to tomorrow, [updateNextDays] is called. You need to implement proper logic that might call [updateTasks],
 * so that tasks for the next day are properly handled.
 */
abstract class AlarmManagerTaskScheduler<T : TaskItem>(
	initialTasks: List<T>,
	val context: Context,
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
	
	/**
	 * This function should be idempotent, so that it can be used to [AlarmManager.cancel].
	 */
	abstract fun schedulerIntent(schedule: TaskSchedule): PendingIntent
	
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
		
		val today = dayOf(currentTimeMillis())
		val updateTomorrow = if(next == null) {
			true
		} else {
			dayOf(next.timeMillis) > today
		}
		if(updateTomorrow) updateNextDays(previousDay = today)
		
		val newNext = schedules.getOrNull(index + 1) // maybe updated from next, from updateTomorrow
		if(newNext != null) {
			scheduleAlarm(newNext)
		}
		
		super.onSchedule(schedule, coroutineScope)
	}
	
	protected abstract fun updateNextDays(previousDay: Long)
	
	override fun onScheduleUpdated() {
		val new = schedules.firstOrNull()
		val last = alarmSchedule
		
		if(new != last) {
			if(last != null) {
				cancelAlarm(last)
			}
			
			if(new != null) {
				scheduleAlarm(new)
			}
			
			alarmSchedule = new
		}
	}
	
	private val intentCache = mutableMapOf<TaskSchedule, PendingIntent>()
	
	private fun schedulerIntentCached(schedule: TaskSchedule) =
		intentCache.getOrPut(schedule) { schedulerIntent(schedule) }
	
	private fun scheduleAlarm(schedule: TaskSchedule) {
		debugFlow("scheduleAlarm $schedule")
		val intent = schedulerIntentCached(schedule)
		val timeMillis = schedule.timeMillis
		
		if(Build.VERSION.SDK_INT >= 23) {
			manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, intent)
		} else {
			manager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, intent)
		}
	}
	
	private fun cancelAlarm(schedule: TaskSchedule) {
		debugFlow("cancelAlarm $schedule")
		val intent = schedulerIntentCached(schedule)
		manager.cancel(intent)
		
		intentCache -= schedule // cancelled so cache won't be used
	}
	
	/// Etc
	
	open fun cleanOldSchedules() {
		val current = currentTimeMillis()
		
		val index = schedules.indexOfFirst { it.timeMillis > current }
		if(index > 0) schedules = schedules.drop(index)
	}
}
