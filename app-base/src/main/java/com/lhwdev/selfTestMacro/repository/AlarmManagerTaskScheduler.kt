package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.preferenceInt
import com.lhwdev.selfTestMacro.database.preferenceLong
import com.lhwdev.selfTestMacro.database.preferenceSerialized
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer


/*
 * The most important part of this app.
 * If this fails, UX would be zero.
 * Bugs should never exist in here.
 */


class AlarmManagerTaskSchedulerBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
	}
}


private const val sSetExactInterval = 18 * 60 * 1000 // 18m


/**
 * If this is implemented properly, implementing SelfTestManagerImpl would be EASY, literally.
 *
 * This class is seemingly stateless. If you call [updateTasks], all tasks previous will be cancelled and new tasks
 * will be added externally. Internally, this should handle these updates efficiently.
 */
class AlarmManagerTaskScheduler<T : Any>(
	val context: Context,
	taskSerializer: KSerializer<T>,
	holder: PreferenceHolder = PreferenceHolder(
		context.getSharedPreferences("AlarmManagerTaskScheduler", Context.MODE_PRIVATE)
	)
) : TaskScheduler<T> {
	@Serializable
	private class TaskSchedule(
		val code: Int,
		val timeMillis: Long
	)
	
	
	private val manager = context.getSystemService<AlarmManager>()!!
	
	
	/// Task: lightweight
	
	private var taskId by holder.preferenceLong(
		"taskId",
		defaultValue = Long.MIN_VALUE
	)
	
	override fun nextTaskId(): Long = taskId++
	
	
	private var tasks: List<TaskItem<T>> by holder.preferenceSerialized(
		"tasks",
		serializer = ListSerializer(TaskItem.serializer(taskSerializer)),
		defaultValue = emptyList()
	)
	
	override val allTasks: List<TaskItem<T>> get() = tasks
	
	override fun updateTasks(tasks: List<TaskItem<T>>) {
		val lastTasks = this.tasks
		if(lastTasks == tasks) return
		
		val newTasks = tasks.sortedBy { it.timeMillis }
		this.tasks = newTasks
		
		if(lastTasks == newTasks) return
		
		if(newTasks.isEmpty()) {
			schedules.forEach { scheduleCancel(it) }
			schedules = emptyList()
			return
		}
		
		val lastSchedules = ArrayDeque(schedules)
		var currentSchedule = lastSchedules.removeFirstOrNull()
			?: TaskSchedule(code = nextScheduleId(), timeMillis = newTasks.first().timeMillis)
		val newSchedules = mutableListOf(currentSchedule)
		
		// Tasks are cheap, need not diff or anything, maybe?
		for(task in newTasks) {
			// reuse current existing schedule
			if(canTaskScheduled(task, currentSchedule)) {
				continue
			}
			
			val next = lastSchedules.first()
			val resolved = if(canTaskScheduled(task, schedule = next)) {
				// reuse next existing schedule
				lastSchedules.removeFirst()
				next
			} else {
				// create new schedule
				while(lastSchedules.first().timeMillis < task.timeMillis) {
					val remove = lastSchedules.removeFirst()
					scheduleCancel(remove)
				}
				val new = TaskSchedule(code = nextScheduleId(), timeMillis = task.timeMillis)
				scheduleSet(new.timeMillis)
				new
			}
			newSchedules += resolved
		}
	}
	
	private fun canTaskScheduled(task: TaskItem<T>, schedule: TaskSchedule): Boolean =
		task.timeMillis - schedule.timeMillis > sSetExactInterval
	
	
	/// Schedules: low level, heavy task; many tasks can be run in one schedule.
	///            created to respond Android scheduling restriction.
	
	private var schedules: List<TaskSchedule> by holder.preferenceSerialized(
		"schedules",
		serializer = ListSerializer(TaskSchedule.serializer()),
		defaultValue = emptyList()
	)
	
	private var scheduleId by holder.preferenceInt(
		"scheduleId",
		defaultValue = Int.MIN_VALUE
	)
	
	private fun nextScheduleId() = scheduleId++
	
	private val schedulerIntent = Intent(context, AlarmManagerTaskSchedulerBroadcastReceiver::class.java)
	private val schedulerFlags = PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
		PendingIntent.FLAG_IMMUTABLE
	} else {
		0
	}
	
	// This function should be idempotent, so that it can be used to AlarmManager.cancel
	private fun schedulerIntent(code: Int): PendingIntent =
		PendingIntent.getBroadcast(context, code, schedulerIntent, schedulerFlags)
	
	
	/// Unsafe schedule operations: managing [schedules] or related [tasks] is not related
	
	private fun scheduleSet(time: Long): TaskSchedule {
		val code = nextScheduleId()
		val intent = schedulerIntent(code = code)
		
		if(Build.VERSION.SDK_INT >= 23) {
			manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
		} else {
			manager.setExact(AlarmManager.RTC_WAKEUP, time, intent)
		}
		return TaskSchedule(code = code, timeMillis = time)
	}
	
	private fun scheduleCancel(schedule: TaskSchedule) {
		val intent = schedulerIntent(code = schedule.code)
		manager.cancel(intent)
	}
	
	
	/// Etc
	
	fun cleanOldTasks() {
		val current = System.currentTimeMillis()
		
		run {
			val index = tasks.indexOfFirst { it.timeMillis > current }
			if(index != 0) tasks = tasks.drop(index)
		}
		
		run {
			val index = schedules.indexOfFirst { it.timeMillis > current }
			if(index != 0) schedules = schedules.drop(index)
		}
	}
}
