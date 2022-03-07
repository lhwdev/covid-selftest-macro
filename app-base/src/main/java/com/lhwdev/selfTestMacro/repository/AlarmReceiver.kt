package com.lhwdev.selfTestMacro.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.debug.*
import com.lhwdev.selfTestMacro.debuggingWithIde
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AlarmReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val scheduleCode = intent.getIntExtra("code", -1)
		
		val selfTestManager = context.defaultSelfTestManager {
			it.createDefaultSelfTestManager(
				debugContext = BackgroundDebugContext(
					flags = DebugContext.DebugFlags(
						enabled = context.isDebugEnabled,
						debuggingWithIde = App.debuggingWithIde
					),
					manager = context.debugManager,
					contextName = "SelfTestSchedule"
				)
			)
		}
		val schedule = selfTestManager.schedules.getSchedule(scheduleCode) ?: run {
			selfTestManager.debugContext.onLightError(
				"자가진단 예약이 실행되려 했지만, 해당하는 일정을 찾을 수 없어요.",
				diagnostics = diagnosticElements {
					"scheduleCode" to scheduleCode
					"schedules" to selfTestManager.schedules
				}
			)
			return
		}
		if(schedule.tasks.size > 1) {
			val serviceIntent = Intent(context, ScheduleService::class.java)
			serviceIntent.putExtra("code", scheduleCode)
			context.startService(serviceIntent)
		} else {
			val pendingResult = goAsync()
			
			@OptIn(DelicateCoroutinesApi::class) // valid case: we can use default
			GlobalScope.launch {
				selfTestManager.onSubmitSchedule(schedule)
				pendingResult.finish()
			}
		}
	}
}
