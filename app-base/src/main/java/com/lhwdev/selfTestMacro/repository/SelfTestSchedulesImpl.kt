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

internal fun dayOf(millis: Long) = millis / sDayMillis // NOTE: how about leap second? It may be affected
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
 * [SelfTestSchedules] also saves task list in [tasks] which is used by schedulers. All schedule creation and update is
 * handled by this class.
 *
 * [tasks] are reset every day.
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
	
	
	/**
	 * Note that this task is not sorted by [SelfTestTask.timeMillis].
	 */
	final override fun updateAndGetTasks(): List<SelfTestTask> {
		val today = today()
		return if(targetDay != today) {
			// lastDay may be intentionally set to the next day from [updateTomorrow].
			if(targetDay < today) {
				targetDay = today
			}
			
			createTasks().also {
				tasksCache = it
				scheduleLog { "updateAndGetTasks: ${dumpDebug(oneLine = false)}" }
			}
		} else {
			tasksCache
		}
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
	
	private fun createTasks(): List<SelfTestTask> {
		val list = ArrayList<SelfTestTask>(database.users.users.size)
		
		val old = ArrayDeque(tasksCache)
		val new = ArrayList<SelfTestTask>(/* initialCapacity = */ old.size)
		var modified = false
		
		outer@ for(group in database.testGroups.groups.values) {
			val schedule = group.schedule
			if(schedule == DbTestSchedule.None) continue
			
			val timeRange = group.nextTime()
			
			when {
				schedule !is DbTestSchedule.Random -> { // stable!
					check(timeRange.first == timeRange.last) // 'stable'
					
					val last = old.removeFirstOrNull()
					if(last != null && last.testGroupId == group.id && last.userId == null && last.timeMillis in timeRange) {
						new += last
						continue
					}
					list += SelfTestTask(
						testGroupId = group.id,
						userId = null, // because the time is stable, we can do all the users at once.
						timeMillis = timeRange.first
					)
				}
				schedule.altogether -> { // unstable
					// remove left group tasks (last one + in case database is changed)
					
					var last: SelfTestTask?
					while(true) {
						last = old.firstOrNull()
						if(last == null || last.testGroupId != group.id) break
						
						old.removeFirst()
						
						if(last.userId == null && last.timeMillis in timeRange) {
							new += last
							continue@outer
						} else {
							modified = true
						}
					}
					
					val timeMillis = timeRange.random(random)
					new += SelfTestTask(
						testGroupId = group.id,
						userId = null,
						timeMillis = timeMillis
					)
				}
				else -> { // unstable
					// try to retrieve from old; if users database is changed, update all
					var i = 0
					val users = with(database) { group.target.allUsers }
					
					while(i < users.size) {
						val next = old.firstOrNull() ?: break
						val user = users[i]
						
						if(next.testGroupId != group.id) {
							break
						}
						
						if(
							next.userId == user.id && // if same user
							next.timeMillis in timeRange // if schedule is not changed
						) {
							// matched
							old.removeFirst()
							new += next
							i++
							continue
						}
						
						// not match, but from same test group
						old.removeFirst()
						modified = true
					}
					
					// if not complete
					while(i < users.size) {
						val user = users[i]
						val timeMillis = timeRange.random(random)
						new += SelfTestTask(
							testGroupId = group.id,
							userId = user.id,
							timeMillis = timeMillis
						)
						
						modified = true
						i++
					}
				}
				
			}
		}
		
		if(modified) {
			tasksCache = new
			scheduler.updateTasks(new)
		}
		
		return list
	}
	
	
	fun updateStatus(group: DbTestGroup, users: List<DbUser>?, results: List<SubmitResult>, logRange: IntRange) {
		val tasks = updateAndGetTasks()
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
			val result = results.getOrNull(index)
			
			val logId = when(result) {
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
	
	
	/// scheduler implementation
	
	protected abstract suspend fun onScheduledSubmitSelfTest(group: DbTestGroup, users: List<DbUser>?)
	
	
	val scheduler = object : AlarmManagerTaskScheduler<SelfTestTask>(
		initialTasks = updateAndGetTasks(),
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
			updateAndGetTasks()
		}
	}
	
	override fun getSchedule(code: Int): Schedule? = scheduler.getSchedule(code)?.let { Schedule(it) }
	
	
	// Diagnostic
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("SelfTestSchedulesImpl") {
		"tasks" set diagnosticGroup("tasks") {
			val schedules = ArrayDeque(scheduler.schedules)
			for((index, task) in scheduler.allTasks.withIndex()) {
				while(!scheduler.canTaskScheduled(task, schedules.first())) schedules.removeFirst()
				if(schedules.isEmpty()) break
				
				"$index" set diagnosticGroup("taskEntry") {
					"schedule" set schedules.first().code
					"task" set task
				}
			}
		}
	}
}
