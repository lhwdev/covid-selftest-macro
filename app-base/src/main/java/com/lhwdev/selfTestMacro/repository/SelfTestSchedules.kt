package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbUser
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
	companion object {
		fun identity(testGroupId: Int, userId: Int?): Long =
			(testGroupId.toLong() shl 32) or (userId?.toLong() ?: 0L)
	}
	
	@Serializable
	class TaskResult(
		val errorLogId: Int? = null // -1 to no log
	)
	
	/**
	 * A unique key to identify a schedule.
	 * If this [identity] is same, a schedule is not run for same day unless user explicitly allows.
	 *
	 * @see SelfTestTask.Companion.identity
	 */
	val identity: Long
		get() = identity(testGroupId, userId)
	
	val done: Boolean
		get() = result != null
	
	override val ignoredByScheduler: Boolean
		get() = done
	
	override fun toString(): String =
		"SelfTestTask(testGroupId=$testGroupId, userId=$userId, timeMillis=${timeMillis.millisToDeltaString()}, result=$result)"
}


abstract class SelfTestSchedules {
	abstract val tasksCache: List<SelfTestTask>
	
	abstract fun updateAndGetTasks(): List<SelfTestTask>
	
	abstract fun getTask(testGroup: DbTestGroup, user: DbUser?): SelfTestTask?
	
	abstract fun getTasks(testGroup: DbTestGroup): List<SelfTestTask>
	
	abstract fun getSchedule(code: Int): SelfTestSchedule?
}


interface SelfTestSchedule {
	val code: Int
	
	val tasks: List<SelfTestTask>
}
