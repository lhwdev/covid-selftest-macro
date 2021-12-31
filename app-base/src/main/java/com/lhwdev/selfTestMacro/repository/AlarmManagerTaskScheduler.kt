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
import com.lhwdev.selfTestMacro.database.preferenceSerialized
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer


private var schedulerIntent: PendingIntent? = null

private fun Context.schedulerIntent(): PendingIntent = schedulerIntent ?: run {
	val intent = PendingIntent.getBroadcast(
		this,
		100,
		Intent(this, AlarmManagerTaskSchedulerBroadcastReceiver::class.java),
		PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
			PendingIntent.FLAG_IMMUTABLE
		} else {
			0
		}
	)
	schedulerIntent = intent
	intent
}


class AlarmManagerTaskSchedulerBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
	}
}


private const val sSetExactInterval = 15 * 60 * 1000 // 15m


class AlarmManagerTaskScheduler<T : Any>(val context: Context, taskSerializer: KSerializer<T>) : TaskScheduler<T> {
	private val manager = context.getSystemService<AlarmManager>()!!
	
	private val holder =
		PreferenceHolder(context.getSharedPreferences("AlarmManagerTaskScheduler", Context.MODE_PRIVATE))
	
	private var tasks: List<TaskItem<T>> by holder.preferenceSerialized(
		"tasks",
		ListSerializer(TaskItem.serializer(taskSerializer)),
		listOf()
	)
	
	private fun firstTask() = tasks[0]
	
	private fun schedule(time: Long) {
		val intent = context.schedulerIntent()
		
		if(Build.VERSION.SDK_INT >= 23) {
			manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
		} else {
			manager.setExact(AlarmManager.RTC_WAKEUP, time, intent)
		}
	}
	
	override fun queueTasks(tasks: List<TaskItem<T>>) {
		val set = this.tasks.toSortedSet(compareBy { it.timeMillis })
		set += tasks
		this.tasks = set.toList()
	}
	
	
	override fun cancelTask(id: Long) {
		val index = tasks.indexOfFirst { it.taskId == id }
		if(index == -1) return
		
		tasks = tasks.toMutableList().also { it.removeAt(index) }
		if(index == 0) {
			// needs reschedule
			
		}
	}
}
