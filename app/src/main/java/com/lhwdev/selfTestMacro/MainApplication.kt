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
		
		with(FirstInitialization) {
			initializeApplication(
				versionName = BuildConfig.VERSION_NAME,
				versionCode = BuildConfig.VERSION_CODE,
				flavor = BuildConfig.FLAVOR,
				debug = BuildConfig.DEBUG
			)
		}
		
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



