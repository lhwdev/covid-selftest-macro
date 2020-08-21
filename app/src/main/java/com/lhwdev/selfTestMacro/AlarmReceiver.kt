package com.lhwdev.selfTestMacro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Debug
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
	companion object {
		const val TAG = "AlarmReceiver"
		const val REQUEST_CODE = 0
	}
	
	override fun onReceive(context: Context, intent: Intent) {
		context.doSubmit()
		
		val pref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
		val hour by pref.preferenceInt("hour", -1)
		val min by pref.preferenceInt("min", 0)
		context.scheduleNextAlarm(context.createIntent(), hour, min, true)
	}
}