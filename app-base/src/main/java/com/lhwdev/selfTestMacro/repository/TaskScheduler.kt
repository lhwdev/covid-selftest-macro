package com.lhwdev.selfTestMacro.repository


interface TaskItem {
	val timeMillis: Long
	
	fun onScheduled(debugData: Any) {
		println("$this -> $debugData")
	}
}


interface TaskScheduler<T : TaskItem> {
	val allTasks: List<T>
	
	fun updateTasks(tasks: List<T>)
	
	fun nextTaskId(): Long
}
