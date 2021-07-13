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
		
		val lastHandler = Thread.getDefaultUncaughtExceptionHandler()
		
		if(!BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
			runBlocking {
				writeErrorLog(getErrorInfo(exception, "defaultUncaughtExceptionHandler"))
			}
			lastHandler?.uncaughtException(thread, exception)
		}
	}
}
