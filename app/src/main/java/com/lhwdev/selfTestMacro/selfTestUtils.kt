@file:Suppress("SpellCheckingInspection")
@file:JvmName("AndroidSelfTestUtils")

package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.lhwdev.selfTestMacro.api.SurveyData
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.api.getUserGroup
import com.lhwdev.selfTestMacro.api.registerSurvey
import java.util.Calendar


suspend fun Context.singleOfUserGroup(list: List<User>) = if(list.size == 1) list.single() else {
	if(list.isEmpty()) showToastSuspendAsync("사용자를 찾지 못했습니다.")
	else showToastSuspendAsync("아직 여러명의 자가진단은 지원하지 않습니다.")
	null
}

suspend fun Context.submitSuspend(notification: Boolean = true) {
	val institute = preferenceState.institute!!
	val loginInfo = preferenceState.user!!
	try {
		val user = singleOfUserGroup(getUserGroup(institute, loginInfo.token)) ?: return
		
		val result = registerSurvey(
			preferenceState.institute!!,
			user,
			SurveyData(userToken = user.token, userName = user.name)
		)
		if(notification) showTestCompleteNotification(result.registerAt)
		else {
			showToastSuspendAsync("자가진단 제출 완료")
		}
	} catch(e: Throwable) {
		showTestFailedNotification(e.stackTraceToString())
		onError(e, "제출 실패")
	}
}

fun Context.updateTime(intent: PendingIntent) {
	val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
	alarmManager.cancel(intent)
	if(preferenceState.hour != -1)
		scheduleNextAlarm(intent, preferenceState.hour, preferenceState.min)
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
