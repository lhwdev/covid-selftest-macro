package com.lhwdev.selfTestMacro

import android.app.Application
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.runBlocking


@Suppress("unused")
class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		sDummyForInitialization
		sDebugFetch = isDebugEnabled
		
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
