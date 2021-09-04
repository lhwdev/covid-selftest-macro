@file:Suppress("SpellCheckingInspection")
@file:JvmName("AndroidSelfTestUtils")

package com.lhwdev.selfTestMacro.ui

import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestSchedule


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
