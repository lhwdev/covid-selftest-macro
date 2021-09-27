@file:Suppress("SpellCheckingInspection")
@file:JvmName("AndroidSelfTestUtils")

package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lhwdev.selfTestMacro.api.*
import net.gotev.cookiestore.InMemoryCookieStore
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.Calendar
import kotlin.random.Random


fun selfTestSession(context: Context): Session {
	return Session(
		CookieManager(
			/*PersistentCookieStore(
				context.getSharedPreferences(
					"cookie-persistent",
					Context.MODE_PRIVATE
				)
			),*/ InMemoryCookieStore("cookie"),
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
	try {
		tryAtMost(maxTrial = 3) trial@{
			val institute = preferenceState.institute!!
			val loginInfo: UserLoginInfo =
				preferenceState.user!! // (not valid ->) // note: `preferenceStte.user` may change after val user = ...
			
			val isIsolated = preferenceState.isIsolated
			
			// val user = loginInfo.ensureTokenValid(
			// 	session, institute,
			// 	onUpdate = { preferenceState.user = it }
			// ) { token ->
			// 	singleOfUserGroup(session.getUserGroup(institute, token)) ?: return
			// }
			val usersIdentifier = loginInfo.findUser(session)
			
			val usersToken = session.validatePassword(institute, usersIdentifier, loginInfo.password) as? UsersToken
				?: error("잘못된 비밀번호입니다.")
			
			val users = session.getUserGroup(institute, usersToken)
			
			val user = singleOfUserGroup(users) ?: return@trial
			
			val result = session.registerSurvey(
				preferenceState.institute!!,
				user,
				SurveyData(userToken = user.token, upperUserName = user.name, rspns09 = if(isIsolated) "1" else "0")
			)
			
			println("selfTestMacro: submitSuspend=success")
			if(notification) showTestCompleteNotification(result.registerAt)
			else {
				showToastSuspendAsync("자가진단 제출 완료")
			}
		}
	} catch(e: Throwable) {
		showTestFailedNotification(e.stackTraceToString())
		onError(e, "제출 실패")
	}
}

fun Context.updateTime(intent: PendingIntent) {
	val preferenceState = preferenceState
	val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
	alarmManager.cancel(intent)
	if(preferenceState.isSchedulingEnabled)
		scheduleNextAlarm(intent, preferenceState.hour, preferenceState.min, isRandom = preferenceState.isRandomEnabled)
}

private val random = Random

@SuppressLint("NewApi")
fun Context.scheduleNextAlarm(
	intent: PendingIntent,
	hour: Int,
	min: Int,
	isRandom: Boolean,
	nextDay: Boolean = false,
) {
	val newTime = Calendar.getInstance().run {
		val newMin = if(isRandom) (min + random.nextInt(-5, 6)).coerceIn(0, 59) else min
		val new = clone() as Calendar
		new[Calendar.HOUR_OF_DAY] = hour
		new[Calendar.MINUTE] = newMin
		new[Calendar.SECOND] = 0
		new[Calendar.MILLISECOND] = 0
		if(nextDay || new <= this) new.add(Calendar.DAY_OF_YEAR, 1)
		new
	}
	Log.i("SelfTestMacro", "scheduling next alarm at $newTime")
	
	val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
	if(Build.VERSION.SDK_INT < 21) {
		alarmManager.setExact(
			AlarmManager.RTC_WAKEUP,
			newTime.timeInMillis,
			intent
		)
	} else {
		alarmManager.setAlarmClock(
			AlarmManager.AlarmClockInfo(
				newTime.timeInMillis,
				PendingIntent.getActivity(
					this,
					0,
					Intent(this, MainActivity::class.java).also {
						it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					},
					PendingIntent.FLAG_ONE_SHOT or (if(Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else 0)
				)
			),
			intent
		)
	}
	
	Log.i("SelfTestMacro", "scheduled next alarm")
	
}
