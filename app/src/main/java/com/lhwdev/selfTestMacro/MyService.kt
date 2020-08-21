//package com.lhwdev.selfTestMacro
//
//import android.app.AlarmManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Binder
//import android.os.IBinder
//
//class MyService : Service() {
//	lateinit var alarmManager: AlarmManager
//
//
//	override fun onCreate() {
//		super.onCreate()
//
//		alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//	}
//
//	private val binder = LocalBinder()
//
//	inner class LocalBinder : Binder() {
//		// Return this instance of LocalService so clients can call public methods
//		val service get() = this@MyService
//	}
//
//	override fun onBind(intent: Intent) = binder
//}