package com.lhwdev.selfTestMacro.repository

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.debug.*
import com.lhwdev.selfTestMacro.debuggingWithIde
import kotlinx.coroutines.*


// Note: subject to background execution limit (from Android 8.0 / api 26)
class ScheduleService : Service() {
	private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
	
	override fun onBind(intent: Intent): IBinder? = null
	
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if(intent != null) {
			val scheduleCode = intent.getIntExtra("code", -1)
			val selfTestManager = defaultSelfTestManager {
				it.createDefaultSelfTestManager(
					debugContext = BackgroundDebugContext(
						flags = DebugContext.DebugFlags(
							enabled = isDebugEnabled,
							debuggingWithIde = App.debuggingWithIde
						),
						manager = debugManager,
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
				return START_STICKY
			}
			
			coroutineScope.launch {
				selfTestManager.onSubmitSchedule(schedule)
				stopSelf(startId)
			}
		}
		
		return START_STICKY
	}
	
	override fun onDestroy() {
		super.onDestroy()
		coroutineScope.coroutineContext.job.cancel(CancellationException("ScheduleService: onDestroy"))
	}
}
