@file:Suppress("SpellCheckingInspection")
@file:JvmName("AndroidSelfTestUtils")

package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import java.util.Calendar


fun DbTestGroup.scheduleInfo(): String = buildString {
	fun fixed(fixed: DbTestSchedule.Fixed) {
		append("${fixed.hour}시 ${fixed.minute}분")
	}
	
	when(val schedule = schedule) {
		DbTestSchedule.None -> append("꺼짐")
		is DbTestSchedule.Fixed -> {
			append("매일 ")
			fixed(schedule)
		}
		is DbTestSchedule.Random -> {
			fixed(schedule.from)
			append("~")
			fixed(schedule.to)
		}
	}
}


suspend fun Context.submitSuspend(notification: Boolean = true) {
	// val institute = preferenceState.institute!!
	// val loginInfo = preferenceState.user!!
	// try {
	// 	val user = singleOfUserGroup(getUserGroup(institute, loginInfo.token)) ?: return
	//
	// 	val result = registerSurvey(
	// 		preferenceState.institute!!,
	// 		user,
	// 		SurveyData(userToken = user.token, userName = user.name)
	// 	)
	// 	if(notification) showTestCompleteNotification(result.registerAt)
	// 	else {
	// 		showToastSuspendAsync("자가진단 제출 완료")
	// 	}
	// } catch(e: Throwable) {
	// 	showTestFailedNotification(e.stackTraceToString())
	// 	onError(e, "제출 실패")
	// }
}

fun Context.updateTime(intent: PendingIntent) {
	val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
	alarmManager.cancel(intent)
	// if(preferenceState.isSchedulingEnabled)
	// 	scheduleNextAlarm(intent, preferenceState.hour, preferenceState.min)
}

@SuppressLint("NewApi")
fun Context.scheduleNextAlarm(
	intent: PendingIntent,
	hour: Int,
	min: Int,
	nextDay: Boolean = false
) {
	(getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExact(
		AlarmManager.RTC_WAKEUP,
		Calendar.getInstance().run {
			val new = clone() as Calendar
			new[Calendar.HOUR_OF_DAY] = hour
			new[Calendar.MINUTE] = min
			new[Calendar.SECOND] = 0
			new[Calendar.MILLISECOND] = 0
			if(nextDay || new <= this) new.add(Calendar.DAY_OF_YEAR, 1)
			new.timeInMillis
		},
		intent
	)
}
