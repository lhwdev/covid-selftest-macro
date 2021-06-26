package com.lhwdev.selfTestMacro

import android.app.Application
import kotlinx.coroutines.*


@Suppress("unused")
class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		sDummyForInitialization
		sDebugFetch = false
		
		// debug code
		System.setProperty(
			DEBUG_PROPERTY_NAME,
			if(BuildConfig.DEBUG) DEBUG_PROPERTY_VALUE_ON else DEBUG_PROPERTY_VALUE_OFF
		)
		
		Thread.setDefaultUncaughtExceptionHandler { _, exception ->
			runBlocking {
				writeErrorLog(getErrorInfo(exception, "defaultUncaughtExceptionHandler"))
			}
		}
	}
}
