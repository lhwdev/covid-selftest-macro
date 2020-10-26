package com.lhwdev.selfTestMacro

import android.app.Application


@Suppress("unused")
class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		sDummyForInitialization
		sDebugFetch = BuildConfig.DEBUG
	}
}
