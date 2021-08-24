package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lhwdev.selfTestMacro.database.createIntent


class BootReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
		context.updateTime(context.createIntent())
	}
}
