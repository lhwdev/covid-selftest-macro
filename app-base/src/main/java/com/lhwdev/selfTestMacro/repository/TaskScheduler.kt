package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.utils.DynamicSerializable
import kotlinx.serialization.Serializable
import kotlin.random.Random


@Serializable
class TaskItem<T : Any>(
	val task: DynamicSerializable<T>,
	val timeMillis: Long,
	val taskId: Long = Random.nextLong()
)


interface TaskScheduler<T : Any> {
	fun queueTasks(tasks: List<TaskItem<T>>)
	
	fun cancelTask(id: Long)
}
