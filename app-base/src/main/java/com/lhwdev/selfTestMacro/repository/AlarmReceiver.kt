package com.lhwdev.selfTestMacro.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.debug.BackgroundDebugContext
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.debugManager
import com.lhwdev.selfTestMacro.debug.isDebugEnabled
import com.lhwdev.selfTestMacro.debuggingWithIde

class AlarmReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val lock = context.getSystemService<PowerManager>()!!
			.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
				"SelfTestMacro:AlarmReceiver"
			)
		lock.acquire(20000)
		
		val selfTestManager = context.defaultSelfTestManager {
			it.createDefaultSelfTestManager(
				debugContext = BackgroundDebugContext(
					flags = DebugContext.DebugFlags(
						enabled = context.isDebugEnabled,
						debuggingWithIde = App.debuggingWithIde
					),
					manager = context.debugManager,
					contextName = "AlarmReceiver"
				)
			)
		}
	}
}
