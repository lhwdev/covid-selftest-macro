package com.lhwdev.selfTestMacro.repository

import kotlinx.serialization.Serializable


@Serializable
data class SelfTestTask(
	val testGroupId: Int,
	
	val userId: Int? = null, // null if schedule.stable
	
	override var timeMillis: Long,
	
	/**
	 * If null, means task is not completed yet. Otherwise stands for result of task execution.
	 */
	var result: TaskResult? = null // generally should be 'val'. see updateStatus.
) : TaskItem {
	@Serializable
	class TaskResult(
		val errorLogId: Int? = null // -1 to no log
	)
	
	override val ignoredByScheduler: Boolean
		get() = result != null
}


abstract class SelfTestSchedules {
	abstract val tasksCache: List<SelfTestTask>
	
	abstract fun updateAndGetTasks(): List<SelfTestTask>
	
	abstract fun getSchedule(code: Int): SelfTestSchedule?
}


interface SelfTestSchedule {
	val code: Int
	
	val tasks: List<SelfTestTask>
}
