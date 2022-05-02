package com.lhwdev.selfTestMacro.utils

import com.lhwdev.selfTestMacro.repository.dayOf
import java.util.Calendar
import kotlin.math.abs


// fun Long.millisToDuration(now: Long = System.currentTimeMillis()): Duration =
// 	with(Duration) { (this@millisToDuration - now).milliseconds }


fun Long.millisToLocalizedString(now: Long = System.currentTimeMillis()): String = StringBuilder().also {
	val date = Calendar.getInstance()
	date.timeInMillis = this
	val days = dayOf(now) - dayOf(this)
	
	if(days != 0L) {
		val delta = if(days > 0L) "후" else "전"
		it.append("${abs(days)}일 $delta ")
	}
	
	it.append("${date[Calendar.HOUR_OF_DAY]}:${date[Calendar.MINUTE]}:${date[Calendar.SECOND]}")
}.toString()
