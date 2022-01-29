package com.lhwdev.selfTestMacro.repository


interface TaskItem {
	val timeMillis: Long
}


interface TaskScheduler<T : TaskItem> {
	/**
	 * This may contain old tasks.
	 */
	val allTasks: List<T>
	
	fun updateTasks(tasks: List<T>)
	
	fun nextTaskId(): Long
}
