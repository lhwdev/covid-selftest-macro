package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.database.PreferenceHolder
import com.lhwdev.selfTestMacro.database.PreferenceState
import com.lhwdev.selfTestMacro.database.prefMain
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.BackgroundDebugContext
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.debugManager
import com.lhwdev.selfTestMacro.debug.isDebugEnabled
import com.lhwdev.selfTestMacro.ui.SelfTestManager
import kotlinx.coroutines.runBlocking


class AlarmReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val result = goAsync()
		
		val lock = context.getSystemService<PowerManager>()!!
			.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
				"SelfTestMacro:AlarmReceiver"
			)
		lock.acquire(20000)
		
		val selfTestManager = SelfTestManager(
			context = context,
			database = context.preferenceState.db,
			debugContext = BackgroundDebugContext(
				flags = DebugContext.DebugFlags(
					enabled = context.isDebugEnabled,
					debuggingWithIde = App.debuggingWithIde
				),
				manager = context.debugManager,
				contextName = "AlarmReceiver"
			)
		)
		
		runBlocking { // TODO: is this okay?
			
			context.checkAndNotifyUpdate()
		}
		
		val pref = PreferenceState(PreferenceHolder(context.prefMain()))
		// context.scheduleNextAlarm(context.createIntent(), pref.hour, pref.min, true)
		
		lock.release()
		result.finish()
	}
}
