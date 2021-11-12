package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.PreferenceState
import com.lhwdev.selfTestMacro.database.prefMain
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.BackgroundDebugContext
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.isDebugEnabled
import com.lhwdev.selfTestMacro.ui.SelfTestManager
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val result = goAsync()
		val selfTestManager = SelfTestManager(
			context,
			context.preferenceState.db,
			BackgroundDebugContext(
				flags = DebugContext.DebugFlags(
					enabled = context.isDebugEnabled,
					debuggingWithIde = App.debuggingWithIde
				)
			)
		)
		
		runBlocking { // TODO: is this okay?
			with(selfTestManager) {
				// context.submitSelfTestNow()
				TODO()
			}
			context.checkAndNotifyUpdate()
			
			result.finish()
		}
		
		val pref = PreferenceState(PreferenceHolder(context.prefMain()))
		// context.scheduleNextAlarm(context.createIntent(), pref.hour, pref.min, true)
	}
}
