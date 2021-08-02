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
import java.net.CookieManager
import java.net.CookiePolicy
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

fun selfTestSession(context: Context): Session {
	return Session(
		CookieManager(
			PersistentCookieStore(
				context.getSharedPreferences(
					"cookie-persistent",
					Context.MODE_PRIVATE
				)
			),
			CookiePolicy.ACCEPT_ALL
		)
	)
}


suspend fun Context.singleOfUserGroup(list: List<User>) = if(list.size == 1) list.single() else {
	if(list.isEmpty()) showToastSuspendAsync("사용자를 찾지 못했습니다.")
	else showToastSuspendAsync("아직 여러명의 자가진단은 지원하지 않습니다.")
	null
}


suspend fun Context.submitSuspend(session: Session, notification: Boolean = true) {
	// try {
	// 	tryAtMost(maxTrial = 3) {
	// 		val institute = preferenceState.institute!!
	// 		val loginInfo = preferenceState.user!! // note: `preferenceStte.user` may change after val user = ...
	//
	// 		val user = loginInfo.ensureTokenValid(
	// 			session, institute,
	// 			onUpdate = { preferenceState.user = it }
	// 		) { token ->
	// 			singleOfUserGroup(session.getUserGroup(institute, token)) ?: return
	// 		}
	//
	// 		val result = session.registerSurvey(
	// 			preferenceState.institute!!,
	// 			user,
	// 			SurveyData(userToken = user.token, upperUserName = user.name)
	// 		)
	// 		println("selfTestMacro: submitSuspend=success")
	// 		if(notification) showTestCompleteNotification(result.registerAt)
	// 		else {
	// 			showToastSuspendAsync("자가진단 제출 완료")
	// 		}
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
