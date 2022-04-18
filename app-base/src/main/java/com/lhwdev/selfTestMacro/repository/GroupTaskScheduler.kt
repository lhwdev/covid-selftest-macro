package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.debug.TraceItems
import com.lhwdev.selfTestMacro.debug.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/*
 * The most important part of this app.
 * If this fails, UX would be zero.
 * Bugs should never exist in here.
 */



/**
 * Groups lightweight **tasks** and assigns to heavy **schedules**. Schedule may be a platform-dependant thing like
 * an entry in AlarmManager just like implemented in [AlarmManagerTaskScheduler].
 * Sometimes, schedules cannot be freely created, just like Threads, so we 'group' tasks, which is like coroutines.
 *
 * This class is seemingly less stateful. If you call [updateTasks], all tasks previous will be cancelled and new tasks
 * will be added externally. Internally, this should handle these updates efficiently.
 *
 * > note from old days of me: _If this is implemented properly, implementing SelfTestManagerImpl would be EASY, literally._
 *
 * Why I thought so?
 */
@TraceItems
abstract class GroupTaskScheduler<T : TaskItem>(initialTasks: List<T>) : TaskScheduler<T> {
	@Serializable
	data class TaskSchedule(
		val code: Int,
		val timeMillis: Long
	) {
		override fun toString(): String = "TaskSchedule(code=$code, timeMillis=${timeMillis.millisToDeltaString()})"
	}
	
	
	/// Task: lightweight
	
	/**
	 * This should be sorted by [TaskItem.timeMillis].
	 */
	private var tasks: List<T> = initialTasks
	
	override val allTasks: List<T> get() = tasks
	
	
	open fun currentTimeMillis(): Long = System.currentTimeMillis()
	
	final override fun updateTasks(tasks: List<T>) {
		val lastTasks = this.tasks
		if(lastTasks == tasks) return
		
		val newTasks = tasks.sortedBy { it.timeMillis }
		this.tasks = newTasks
		
		// Fast path #1
		if(lastTasks == newTasks) return
		
		// Fast path #2
		if(newTasks.isEmpty()) {
			schedules.forEach { scheduleCancel(it) }
			schedules = emptyList()
			return
		}
		
		val now = currentTimeMillis()
		
		val lastSchedules = ArrayDeque(schedules)
		var currentSchedule = lastSchedules.removeFirstOrNull()
			?: scheduleSet(newTasks.first().timeMillis)
		val newSchedules = mutableListOf(currentSchedule)
		
		// Tasks are cheap, need not diff or anything, maybe?
		outer@ for(task in newTasks) {
			if(task.ignoredByScheduler) continue
			
			while(currentSchedule.timeMillis <= now) {
				currentSchedule = lastSchedules.removeFirstOrNull() ?: break@outer
			}
			
			// reuse current existing schedule
			if(canTaskScheduled(task, currentSchedule)) {
				onTaskScheduled(task, currentSchedule)
				continue
			}
			
			while(lastSchedules.isNotEmpty() && lastSchedules.first().timeMillis < task.timeMillis) {
				val last = lastSchedules.removeFirst()
				scheduleCancel(last)
				scheduleLog { "remove schedule $last" }
			}
			
			val next = lastSchedules.firstOrNull()
			
			val resolved = if(next != null && canTaskScheduled(task, schedule = next)) {
				// reuse next existing schedule
				lastSchedules.removeFirst()
				next
			} else {
				// create new schedule
				scheduleSet(task.timeMillis)
			}
			newSchedules += resolved
			currentSchedule = resolved
			onTaskScheduled(task, resolved)
		}
		
		schedules = newSchedules
		scheduleLog { "GroupTaskScheduler: tasks updated newSchedules=$newSchedules" }
		onScheduleUpdated()
	}
	
	protected open fun onTaskScheduled(task: T, schedule: TaskSchedule) {}
	
	protected abstract suspend fun onTask(task: T)
	
	fun tasksForSchedule(schedule: TaskSchedule): List<T> {
		val index = tasks.indexOfFirst { it.timeMillis >= schedule.timeMillis }
		return tasks.drop(index).takeWhile { canTaskScheduled(it, schedule) }
	}
	
	open suspend fun onSchedule(schedule: TaskSchedule, coroutineScope: CoroutineScope) {
		val index = tasks.indexOfFirst { it.timeMillis >= schedule.timeMillis }
		if(index != 0) {
			error("[GroupTaskScheduler] onSchedule: schedule order miss: target task $index != 0")
		}
		val currentTasks = tasks.takeWhile { canTaskScheduled(it, schedule) }
		
		if(currentTasks.isEmpty()) {
			log("[GroupTaskScheduler] onSchedule: currentTasks is empty")
			return
		}
		
		var last = currentTasks.first()
		for(task in currentTasks) {
			val interval = task.timeMillis - last.timeMillis
			delay(interval)
			
			coroutineScope.launch { onTask(task) }
			
			last = task
		}
		
		tasks = tasks.drop(currentTasks.size)
	}
	
	/**
	 * Note: this should be fast, stateless.
	 */
	abstract fun canTaskScheduled(task: T, schedule: TaskSchedule): Boolean
	
	
	/// Schedules: low level, heavy task; many tasks can be run in one schedule.
	///            created to cope with Android scheduling restriction.
	
	abstract var schedules: List<TaskSchedule>
	
	fun getSchedule(code: Int): TaskSchedule? = schedules.find { it.code == code }
	
	protected abstract var scheduleId: Int
	
	protected open fun nextScheduleId(): Int = scheduleId++
	
	/// Unsafe schedule operations: managing [schedules] or related [tasks] is not related
	
	protected abstract fun scheduleSet(time: Long): TaskSchedule
	
	protected abstract fun scheduleCancel(schedule: TaskSchedule)
	
	protected abstract fun onScheduleUpdated()
}
