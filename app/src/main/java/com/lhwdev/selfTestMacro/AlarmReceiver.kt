package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
	companion object {
		const val REQUEST_CODE = 0
	}
	
	override fun onReceive(context: Context, intent: Intent) {
		val result = goAsync()
		val session = selfTestSession(context)
		
		runBlocking { // TODO: is this okay?
			context.submitSuspend(session)
			result.finish()
		}
		
		val pref = PreferenceState(context.prefMain())
		context.scheduleNextAlarm(context.createIntent(), pref.hour, pref.min, true)
	}
}
