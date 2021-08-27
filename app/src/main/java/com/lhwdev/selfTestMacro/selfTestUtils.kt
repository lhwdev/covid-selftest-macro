@file:Suppress("SpellCheckingInspection")
@file:JvmName("AndroidSelfTestUtils")

package com.lhwdev.selfTestMacro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestSchedule
import net.gotev.cookiestore.InMemoryCookieStore
import java.net.CookieManager
import java.net.CookiePolicy


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
	if(list.isEmpty()) showToastSuspendAsync("사용자를 찾지 못했어요.")
	else showToastSuspendAsync("아직 여러명의 자가진단은 지원하지 않아요.")
	null
}


suspend fun Context.submitSuspend(session: Session, notification: Boolean = true) {
	// try {
	// 	tryAtMost(maxTrial = 3) trial@{
	// 		val institute = preferenceState.institute!!
	// 		val loginInfo: UserLoginInfo =
	// 			preferenceState.user!! // (not valid ->) // note: `preferenceStte.user` may change after val user = ...
	//
	// 		// val user = loginInfo.ensureTokenValid(
	// 		// 	session, institute,
	// 		// 	onUpdate = { preferenceState.user = it }
	// 		// ) { token ->
	// 		// 	singleOfUserGroup(session.getUserGroup(institute, token)) ?: return
	// 		// }
	// 		val usersIdentifier = loginInfo.findUser(session)
	//
	// 		val usersToken = session.validatePassword(institute, usersIdentifier, loginInfo.password) as? UsersToken
	// 			?: error("잘못된 비밀번호입니다.")
	//
	// 		val users = session.getUserGroup(institute, usersToken)
	//
	// 		val user = singleOfUserGroup(users) ?: return@trial
	//
	// 		val result = session.registerSurvey(
	// 			preferenceState.institute!!,
	// 			user,
	// 			SurveyData(userToken = user.token, upperUserName = user.name)
	// 		)
	//
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
