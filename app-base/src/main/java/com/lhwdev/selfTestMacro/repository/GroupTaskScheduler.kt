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
 * If this is implemented properly, implementing SelfTestManagerImpl would be EASY, literally.
 *
 * This class is seemingly less stateful. If you call [updateTasks], all tasks previous will be cancelled and new tasks
 * will be added externally. Internally, this should handle these updates efficiently.
 */
@TraceItems
abstract class GroupTaskScheduler<T : TaskItem> : TaskScheduler<T> {
	@Serializable
	protected data class TaskSchedule(
		val code: Int,
		val timeMillis: Long
	)
	
	
	/// Task: lightweight
	
	protected abstract var taskId: Long
	
	override fun nextTaskId(): Long = taskId++
	
	
	/**
	 * This should be sorted by [TaskItem.timeMillis].
	 */
	protected abstract var tasks: List<T>
	
	override val allTasks: List<T> get() = tasks
	
	final override fun updateTasks(tasks: List<T>) {
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
			?: scheduleSet(newTasks.first().timeMillis)
		val newSchedules = mutableListOf(currentSchedule)
		
		// Tasks are cheap, need not diff or anything, maybe?
		for(task in newTasks) {
			// println("step $task current=$currentSchedule new=$newSchedules last=$lastSchedules")
			
			// reuse current existing schedule
			if(canTaskScheduled(task, currentSchedule)) {
				onTaskScheduled(task, currentSchedule)
				continue
			}
			
			while(lastSchedules.isNotEmpty() && lastSchedules.first().timeMillis < task.timeMillis) {
				val last = lastSchedules.removeFirst()
				scheduleCancel(last)
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
	}
	
	protected open fun onTaskScheduled(task: T, schedule: TaskSchedule) {}
	
	protected abstract suspend fun onTask(task: T)
	
	protected suspend fun onSchedule(schedule: TaskSchedule, coroutineScope: CoroutineScope) {
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
	protected abstract fun canTaskScheduled(task: T, schedule: TaskSchedule): Boolean
	
	
	/// Schedules: low level, heavy task; many tasks can be run in one schedule.
	///            created to respond Android scheduling restriction.
	
	protected abstract var schedules: List<TaskSchedule>
	
	protected abstract var scheduleId: Int
	
	protected open fun nextScheduleId(): Int = scheduleId++
	
	/// Unsafe schedule operations: managing [schedules] or related [tasks] is not related
	
	protected abstract fun scheduleSet(time: Long): TaskSchedule
	
	protected abstract fun scheduleCancel(schedule: TaskSchedule)
}
