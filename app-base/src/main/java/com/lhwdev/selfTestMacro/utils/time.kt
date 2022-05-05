package com.lhwdev.selfTestMacro.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.lhwdev.selfTestMacro.repository.dayOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs


// fun Long.millisToDuration(now: Long = System.currentTimeMillis()): Duration =
// 	with(Duration) { (this@millisToDuration - now).milliseconds }


fun Calendar.headToLocalizedString(
	now: Long = System.currentTimeMillis()
): String = StringBuilder().also {
	
	when(val days = dayOf(now) - dayOf(timeInMillis)) {
		0L -> it.append("오늘")
		1L -> it.append("내일")
		
		else -> {
			val delta = if(days > 0L) "후" else "전"
			it.append("${abs(days)}일 $delta")
		}
	}
}.toString()

fun Calendar.tailToLocalizedString(
	now: Long = System.currentTimeMillis()
): String = StringBuilder().also {
	it.append("${this[Calendar.HOUR_OF_DAY]}:${this[Calendar.MINUTE]}:${this[Calendar.SECOND]}")
}.toString()


@Composable
fun rememberTimeStateOf(unit: TimeUnit): State<Long> = produceState(System.currentTimeMillis()) {
	val duration = TimeUnit.MILLISECONDS.convert(1, unit)
	
	while(isActive) {
		val now = System.currentTimeMillis()
		val nowDiv = now / duration
		val targetDiv = nowDiv + 1
		val target = targetDiv * duration
		delay(target - now)
		value = target
	}
}
