package com.lhwdev.selfTestMacro.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.preferenceSerialized
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer


@Serializable
class SelfTestTask(val testGroupId: Int, val userId: Int, override val timeMillis: Long) : TaskItem


/**
 * This class manages what schedules are going to happen today.
 * We need to save today schedules separately, because:
 *
 * - to prevent random schedule keep changing, so that it is more deterministic, less error-prone, and performant.
 * - to show useful information in [NotificationStatus], like 'self test 3/5 ongoing'
 *
 * [tasks] are cleared every day.
 */
class TodayStatus(holder: PreferenceHolder) {
	var tasks: List<SelfTestTask> by holder.preferenceSerialized(
		"tasks",
		serializer = ListSerializer(SelfTestTask.serializer()),
		defaultValue = emptyList()
	)
	
	
	/**
	 * Clear old tasks, which is from last day.
	 * As past tasks are also used from [NotificationStatus], we do not remove all past tasks, keeping today's tasks.
	 */
	fun clearOldTasks() {
		fun dayOf(millis: Long) = millis / (1000 * 60 * 60 * 24)
		
		val currentDay = dayOf(System.currentTimeMillis())
		
		val index = tasks.indexOfFirst { dayOf(it.timeMillis) >= currentDay }
		if(index > 0) tasks = tasks.drop(index)
	}
}
