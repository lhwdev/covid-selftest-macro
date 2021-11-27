@file:Suppress("SpellCheckingInspection")
@file:JvmName("AndroidSelfTestUtils")

package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import net.gotev.cookiestore.InMemoryCookieStore
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.Calendar
import java.util.Date
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
	val pref = preferenceState
	selfLog("submitSuspend ${pref.user?.identifier?.mainUserName}")
	try {
		tryAtMost(maxTrial = 3) trial@{
			val institute = pref.institute!!
			val loginInfo: UserLoginInfo =
				pref.user!! // (not valid ->) // note: `preferenceStte.user` may change after val user = ...
			
			val isIsolated = pref.isIsolated
			
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
				pref.institute!!,
				user,
				SurveyData(
					userToken = user.token,
					upperUserName = usersIdentifier.mainUserName,
					rspns09 = if(isIsolated) "1" else "0"
				)
			)
			
			pref.lastSubmit = Date().time
			
			selfLog("submitSuspend success ${pref.user?.identifier?.mainUserName} ${result.registerAt}", force = true)
			
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
	if(Build.VERSION.SDK_INT >= 21)
		selfLog("updateTime: lastAlarm=${alarmManager.nextAlarmClock}")
	alarmManager.cancel(intent)
	if(preferenceState.isSchedulingEnabled)
		scheduleNextAlarm(intent, preferenceState.hour, preferenceState.min, isRandom = preferenceState.isRandomEnabled)
}

private val random = Random

private fun millisToDaysCumulative(millis: Long) =
	// ms         s  min hour day
	millis / 1000 / 60 / 60 / 24

@SuppressLint("NewApi")
fun Context.scheduleNextAlarm(
	intent: PendingIntent,
	hour: Int,
	min: Int,
	isRandom: Boolean,
	nextDay: Boolean = false,
) {
	val pref = preferenceState
	val currentTime: Long
	
	var newTime = Calendar.getInstance().run {
		currentTime = timeInMillis
		
		val new = clone() as Calendar
		
		// Submitted today
		val last = pref.lastSubmit
		val lastDay = millisToDaysCumulative(last)
		
		val targetMin = hour * 60 + min
		val currentMin = this[Calendar.HOUR_OF_DAY] * 60 + this[Calendar.MINUTE]
		
		if(
			nextDay ||
			lastDay == millisToDaysCumulative(currentTime) ||
			targetMin < currentMin - 5
		) {
			new.add(Calendar.DAY_OF_YEAR, 1)
		}
		
		new[Calendar.HOUR_OF_DAY] = hour
		new[Calendar.MINUTE] = min
		new[Calendar.SECOND] = 0
		new[Calendar.MILLISECOND] = 0
		new
	}.timeInMillis
	
	if(isRandom) {
		newTime += 1000 * 60 * (Random.nextFloat() * 5).toInt()
		if(newTime - currentTime < 10000) {
			selfLog("scheduling: coerced time from $newTime")
			newTime = currentTime + 10000
		}
	}
	
	selfLog("scheduling next alarm at ${Date(newTime)}", force = true)
	
	val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
	if(Build.VERSION.SDK_INT < 21) {
		alarmManager.setExact(
			AlarmManager.RTC_WAKEUP,
			newTime,
			intent
		)
	} else {
		alarmManager.setAlarmClock(
			AlarmManager.AlarmClockInfo(
				newTime,
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
	
	selfLog("scheduled next alarm! next=${alarmManager.nextAlarmClock}")
}
