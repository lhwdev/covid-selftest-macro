package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.utils.DynamicSerializable
import kotlinx.serialization.Serializable


@Serializable
class TaskItem<T : Any>(
	val task: DynamicSerializable<T>,
	val timeMillis: Long,
	val taskId: Long
)


interface TaskScheduler<T : Any> {
	val allTasks: List<TaskItem<T>>
	
	fun updateTasks(tasks: List<TaskItem<T>>)
	
	fun nextTaskId(): Long
}
