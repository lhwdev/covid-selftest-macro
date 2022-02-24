package com.lhwdev.selfTestMacro.repository

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.lhwdev.selfTestMacro.AndroidNotificationChannel
import com.lhwdev.selfTestMacro.AppNotificationIds
import com.lhwdev.selfTestMacro.AppNotifications
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.database.allUsersCount
import com.lhwdev.selfTestMacro.set
import java.util.Date


/**
 * Manages the notification displayed, such as '5명 중 3명 자가진단 완료', '~~~ 자가진단 실패'.
 */
class NotificationStatus(val schedule: SelfTestSchedule, val database: DatabaseManager, val context: Context) {
	private val notifications = NotificationManagerCompat.from(context)
	
	private val enabled = notifications.areNotificationsEnabled()
	
	private fun isEnabled(channel: AndroidNotificationChannel) = if(Build.VERSION.SDK_INT >= 26) {
		enabled && channel.acquireNotificationChannel(context).importance != NotificationManager.IMPORTANCE_NONE
	} else {
		enabled
	}
	
	// this is to reduce efforts to create notification which would be simply ignored if no such a thing like this didn't exist.
	private val useProgress = isEnabled(AppNotifications.SelfTestProgress)
	private val useSuccess = isEnabled(AppNotifications.SelfTestSuccess)
	private val useFailed = isEnabled(AppNotifications.SelfTestFailed)
	
	
	fun onStatusUpdated(fromUi: Boolean) {
		if(!enabled) return
		val tasks = schedule.tasksCache
		
		var allUsersCount = 0
		var doneCount = 0
		var failedCount = 0
		
		for(task in tasks) {
			val count = if(task.userId == null) {
				val group = database.testGroups.groups[task.testGroupId]
				@Suppress("IfThenToElvis")
				if(group == null) {
					0 // ???
				} else {
					group.target.allUsersCount
				}
			} else {
				1
			}
			
			allUsersCount += count
			
			val result = task.result
			when {
				result == null -> Unit
				result.errorLogId == null -> doneCount++
				else -> failedCount++
			}
		}
		
		if(allUsersCount == doneCount) {
			if(useProgress) notifications[AppNotificationIds.selfTestProgress] = null
			val anyTask = tasks.first()
			val anyTestGroup = database.testGroups.groups.getValue(anyTask.testGroupId)
			val anyName = with(database) { anyTestGroup.target.name }
			
			if(useSuccess && !fromUi) notifications[AppNotificationIds.selfTestSuccess] =
				AppNotifications.SelfTestSuccess.notificationOf(
					context,
					target = if(allUsersCount == 1) {
						anyName
					} else {
						"$anyName 등 ${allUsersCount}명"
					},
					time = if(allUsersCount == 1) {
						Date(anyTask.timeMillis).toString()
					} else {
						null
					}
				)
			return
		}
		
		if(useProgress) notifications[AppNotificationIds.selfTestProgress] =
			AppNotifications.SelfTestProgress.notificationOf(
				context,
				allUsersCount = allUsersCount,
				doneCount = doneCount,
				failedCount = failedCount
			)
	}
	
	fun onSubmitSelfTest(users: List<DbUser>, results: List<SubmitResult>) {
		if(useFailed) for((user, result) in users.zip(results)) {
			if(result is SubmitResult.Failed) {
				notifications[AppNotificationIds.selfTestFailed, "code=${user.userCode}"] =
					AppNotifications.SelfTestFailed.notificationOf(
						context,
						target = user.name,
						message = result.description
					)
			}
		}
	}
}
