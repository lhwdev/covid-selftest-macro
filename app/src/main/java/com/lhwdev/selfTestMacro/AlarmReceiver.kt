package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.PreferenceState
import com.lhwdev.selfTestMacro.database.prefMain
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val result = goAsync()
		val session = selfTestSession(context)
		
		runBlocking { // TODO: is this okay?
			context.submitSuspend(session)
			context.checkUpdate()
			
			result.finish()
		}
		
		val pref = PreferenceState(PreferenceHolder(context.prefMain()))
		// context.scheduleNextAlarm(context.createIntent(), pref.hour, pref.min, true)
	}
}
