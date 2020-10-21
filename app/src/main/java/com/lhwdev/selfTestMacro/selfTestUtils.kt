@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import java.util.Calendar


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
			Log.e("HOI", "$this")
			Log.e("HOI", "$new")
			new.timeInMillis
		},
		intent
	)
}
