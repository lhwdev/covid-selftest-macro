package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
	companion object {
		const val REQUEST_CODE = 0
	}
	
	override fun onReceive(context: Context, intent: Intent) {
		val result = goAsync()
		val lock = context.getSystemService<PowerManager>()!!
			.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
				"SelfTestMacro:AlarmReceiver"
			)
		lock.acquire(10000)
		val session = selfTestSession(context)
		
		runBlocking { // TODO: is this okay?
			context.submitSuspend(session, manual = false)
		}
		
		val pref = PreferenceState(context.prefMain())
		context.scheduleNextAlarm(context.createIntent(), pref.hour, pref.min, pref.isRandomEnabled, nextDay = true)
		lock.release()
		
		result.finish()
	}
}
