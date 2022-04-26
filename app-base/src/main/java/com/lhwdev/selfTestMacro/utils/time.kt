package com.lhwdev.selfTestMacro.utils

import kotlin.time.Duration


fun Long.millisToDuration(now: Long = System.currentTimeMillis()): Duration =
	with(Duration) { (this@millisToDuration - now).milliseconds }


fun Duration.toLocalizedString(): String = toComponents { days, hours, minutes, seconds, _nanoseconds ->
	buildString {
		append("DEBUG ${with(Duration) { 5.hours }}")
		
		var exist = false
		fun item(amount: Int, unit: String) {
			if(amount != 0 || exist) {
				if(amount != 0) exist = true
				append("$amount$unit ")
			}
		}
		
		if(days != 0L) {
			exist = true
			append("${days}일 ")
		}
		item(hours, "시간")
		item(minutes, "분")
		item(seconds, "초")
	}
}
