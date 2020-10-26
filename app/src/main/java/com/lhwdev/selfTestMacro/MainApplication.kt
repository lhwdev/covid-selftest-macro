package com.lhwdev.selfTestMacro

import androidx.multidex.MultiDexApplication


@Suppress("unused")
class MainApplication : MultiDexApplication() {
	override fun onCreate() {
		super.onCreate()
		sDummyForInitialization
		sDebugFetch = BuildConfig.DEBUG
	}
}
