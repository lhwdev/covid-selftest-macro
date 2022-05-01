package com.lhwdev.selfTestMacro.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.debug.*
import kotlinx.serialization.builtins.ListSerializer
import java.util.Calendar
import java.util.Date
import kotlin.random.Random


private const val sDayMillis = 1000 * 60 * 60 * 24

internal fun dayOf(millis: Long) =
	millis / sDayMillis // 'The number of seconds per day are fixed with Unix timestamps.'

internal fun today() = dayOf(System.currentTimeMillis())


var sDebugScheduleEnabled = false

internal inline fun scheduleLog(message: () -> String) {
	if(sDebugScheduleEnabled) Log.d("Schedules", message())
}


private val schedulerFlags = PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
	PendingIntent.FLAG_IMMUTABLE
} else {
	0
}

/**
 * This class manages what **self test schedules** are going to happen in [targetDay].
 * We need to save today schedules separately, because:
 *
 * - to prevent unstable schedules like random schedule keep changing, so that it is more deterministic,
 *   less error-prone, and performant.
 * - to show useful information in [NotificationStatus], like 'self test 3/5 ongoing'
 *
 * [SelfTestSchedules] also saves task list in tasks which is used by schedulers. All schedule creation and update is
 * handled by this class.
 *
 * Tasks are reset every day.
 */
abstract class SelfTestSchedulesImpl(
	context: Context,
	holder: PreferenceHolder,
	private val database: AppDatabase,
	private val debugContext: DebugContext
) : SelfTestSchedules(), DiagnosticObject {
	inner class Schedule(val schedule: GroupTaskScheduler.TaskSchedule) : SelfTestSchedule {
		override val code: Int get() = schedule.code
		override val tasks: List<SelfTestTask> = scheduler.tasksForSchedule(schedule)
		
		override fun equals(other: Any?): Boolean = when {
			this === other -> true
			else -> other is Schedule && schedule == other.schedule
		}
		
		override fun hashCode(): Int = schedule.hashCode()
	}
	
	
	private val random = Random(seed = System.currentTimeMillis())
	
	private val tasksState: PreferenceItemState<List<SelfTestTask>> = holder.preferenceSerialized(
		key = "tasksCache",
		serializer = ListSerializer(SelfTestTask.serializer()),
		defaultValue = emptyList()
	)
	
	// epoch day
	private var targetDay by holder.preferenceLong("targetDay", 0)
	
	private val calendarCache = Calendar.getInstance() // not thread safe (but everything here is also)
	
	
	final override var tasksCache: List<SelfTestTask> by tasksState
		private set
	
	
	// Views
	
	override fun getTask(testGroup: DbTestGroup, user: DbUser?): SelfTestTask? =
		tasksCache.find { it.testGroupId == testGroup.id && it.userId == user?.id }
	
	override fun getTasks(testGroup: DbTestGroup): List<SelfTestTask> =
		tasksCache.filter { it.testGroupId == testGroup.id }
	
	override fun getSchedule(code: Int): Schedule? =
		scheduler.getSchedule(code)?.let { Schedule(it) }
	
	
	/// Schedule Updates
	
	/**
	 * Note that this task is not sorted by [SelfTestTask.timeMillis].
	 */
	final override fun updateTasks(): List<SelfTestTask> {
		val today = today()
		if(targetDay < today) {
			targetDay = today
		}
		
		return createTasks()
		
		// return if(targetDay < today) {
		// 	targetDay = today
		// 	createTasks().also {
		// 		scheduleLog { "updateAndGetTasks: ${dumpDebug(oneLine = false)}" }
		// 	}
		// } else {
		// 	scheduleLog { "updateAndGetTasks: targetDay(=$targetDay) >= today(=$today) so did not update" }
		// 	tasksCache
		// }
	}
	
	/**
	 * Only for debugging purpose, as it resets [targetDay].
	 */
	fun recreateTasks() {
		targetDay = Long.MIN_VALUE
		updateTasks()
	}
	
	fun onScheduleUpdated() {
		scheduleLog { "onScheduleUpdated revision=${database.testGroups.revision}" }
		updateTasks()
	}
	
	private fun DbTestGroup.nextTime(): LongRange {
		// NOTE: calendarCache is cached, so check if it is used synchronously across places. 
		fun calendarFor(schedule: DbTestSchedule.Fixed): Calendar {
			val calendar = calendarCache
			calendar.timeInMillis = targetDay * sDayMillis // reset to the target day
			
			// calendar[Calendar.SECOND] = 0 // as set in `calendar.timeInMillis = ...`
			// calendar[Calendar.MILLISECOND] = 0
			
			calendar[Calendar.HOUR_OF_DAY] = schedule.hour
			calendar[Calendar.MINUTE] = schedule.minute
			return calendar
		}
		
		var calendar: Calendar? = null
		val timeInMillis = when(val schedule = schedule) {
			is DbTestSchedule.Fixed -> {
				calendar = calendarFor(schedule)
				calendar.timeInMillis..calendar.timeInMillis
			}
			is DbTestSchedule.Random -> {
				val from = calendarFor(schedule.from).timeInMillis
				val to = calendarFor(schedule.to).timeInMillis
				from..to
			}
			DbTestSchedule.None -> error("Oh nyooo......")
		}
		
		return if(excludeWeekend) {
			val c = if(calendar == null) {
				calendar = calendarCache
				calendar.timeInMillis = timeInMillis.first // from and to should be on the same day
				calendar
			} else calendar
			
			while(true) {
				when(c[Calendar.DAY_OF_WEEK]) {
					Calendar.SATURDAY -> c.add(Calendar.DAY_OF_YEAR, 2)
					Calendar.SUNDAY -> c.add(Calendar.DAY_OF_YEAR, 1)
					else -> break
				}
			}
			
			val from = c.timeInMillis
			val to = timeInMillis.last + (from - timeInMillis.first)
			from..to
		} else {
			timeInMillis
		}
	}
	
	/**
	 * Tries to preserve old tasks as much as possible, as it is needed to implement 'do not schedule more than once
	 * in a day' behavior.
	 */
	private fun createTasks(): List<SelfTestTask> {
		val oldTasks = tasksCache.associateBy { it.identity }.toSortedMap()
		val usedOldTasks = mutableListOf<SelfTestTask>()
		val newTasks = ArrayList<SelfTestTask>(/* initialCapacity = */ oldTasks.size)
		
		outer@ for(group in database.testGroups.groups.values) {
			val schedule = group.schedule
			if(schedule == DbTestSchedule.None) continue
			
			val timeRange = group.nextTime()
			
			fun getOrCreateTask(user: DbUser?) {
				val identity = SelfTestTask.identity(testGroupId = group.id, userId = user?.id)
				val old = oldTasks[identity]
				val task = if(old != null) {
					usedOldTasks += old
					if(old.timeMillis !in timeRange) {
						old.copy(timeMillis = timeRange.random(random))
					} else {
						old
					}
				} else {
					SelfTestTask(
						testGroupId = group.id,
						userId = user?.id,
						timeMillis = timeRange.random(random)
					)
				}
			}
			
			if(schedule.altogether) { // stable || unstable+altogether
				getOrCreateTask(user = null)
			} else { // unstable
				// try to retrieve from old; if users database is changed, update all
				val users = with(database) { group.target.allUsers }
				
				for(user in users) {
					getOrCreateTask(user = user)
				}
			}
		}
		
		val modified = usedOldTasks.size < oldTasks.size
		if(modified) {
			tasksCache = newTasks
			scheduler.updateTasks(newTasks)
			scheduleLog { "createTasks: modified=true data=" + dumpDebug(oneLine = false) }
		} else {
			scheduleLog { "createTasks: modified=false" }
		}
		
		return newTasks
	}
	
	
	fun updateStatus(group: DbTestGroup, users: List<DbUser>?, results: List<SubmitResult>, logRange: IntRange) {
		val tasks = updateTasks()
		val targetTasks = when {
			group.schedule.altogether ->
				listOfNotNull(tasks.find { it.testGroupId == group.id && it.userId == null })
			
			users == null -> tasks.filter { it.testGroupId == group.id }
			else -> { // note: this is not called generally
				val ids = IntArray(size = users.size) { index -> users[index].id }
				tasks.filter { it.testGroupId == group.id && it.userId != null && it.userId in ids }
			}
		}
		if(targetTasks.isEmpty()) {
			// in case where no scheduling is enabled but manually submitted
			// log("[TodayStatus] updateStatus failed: could not find task with (groupId=$group, users=$users)")
			return
		}
		
		var logIndex = 0
		
		for(index in targetTasks.indices) {
			val task = targetTasks[index]
			
			val logId = when(results.getOrNull(index)) {
				null -> -1
				is SubmitResult.Success -> null
				else -> if(logIndex < logRange.last - logRange.first) {
					logRange.first + logIndex++
				} else {
					-1
				}
			}
			task.result = SelfTestTask.TaskResult(errorLogId = logId)
		}
		
		// SelfTestSchedule.complete is 'var', so it is not synchronized by itself.
		pushDbOperation(this) {
			tasksState.forceWrite()
		}
	}
	
	
	/// Scheduler implementation
	
	protected abstract suspend fun onScheduledSubmitSelfTest(group: DbTestGroup, users: List<DbUser>?)
	
	
	val scheduler = object : AlarmManagerTaskScheduler<SelfTestTask>(
		initialTasks = tasksCache,
		context = context,
		holder = database.holder
	) {
		override fun schedulerIntent(schedule: TaskSchedule): PendingIntent {
			val intent = Intent(context, AlarmReceiver::class.java)
			intent.putExtra("code", schedule.code)
			return PendingIntent.getBroadcast(context, schedule.code, intent, schedulerFlags)
		}
		
		override suspend fun onTask(task: SelfTestTask) {
			val group = database.testGroups.groups[task.testGroupId]
			if(group == null) {
				debugContext.onError(
					message = "예약된 자가진단(시간: ${Date(task.timeMillis)})을 실행하는 중에 사용자 그룹을 찾지 못했습니다.",
					throwable = IllegalStateException("no DbTestGroup with (id=${task.testGroupId})")
				)
				return
			}
			
			val users = if(task.userId == null) {
				// fixed time
				null
			} else {
				// random time + all different
				val user = database.users.users[task.userId]
				if(user == null) {
					debugContext.onError(
						message = "예약된 자가진단(시간: ${Date(task.timeMillis)})을 실행하는 중에 사용자를 찾지 못했습니다.",
						throwable = IllegalStateException("no User with (userId=${task.userId})")
					)
					return
				}
				listOf(user)
			}
			scheduleLog { "onTask: $task group=$group users=$users" }
			
			onScheduledSubmitSelfTest(group, users)
		}
		
		override fun updateNextDays(previousDay: Long) {
			targetDay = previousDay + 1
			updateTasks()
		}
	}
	
	
	// Diagnostic
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("SelfTestSchedulesImpl") {
		"targetDay" set "$targetDay (today: ${today()})"
		"tasks" set diagnosticGroup("tasks") {
			val schedules = ArrayDeque(scheduler.schedules)
			var removedCount = 0
			for((index, task) in tasksCache.withIndex()) {
				val schedule = if(schedules.isEmpty()) {
					null
				} else {
					while(schedules.isNotEmpty() && !scheduler.canTaskScheduled(task, schedules.first())) {
						val removed = schedules.removeFirst()
						"unusedSchedule.${removedCount}" set removed
						removedCount++
					}
					schedules.firstOrNull()
				}
				
				"$index" set diagnosticGroup("TaskEntry") {
					"schedule" set schedule
					"task" set task
				}
			}
		}
		"scheduler" set scheduler
	}
	
	
	init {
		updateTasks()
	}
}
