package com.lhwdev.selfTestMacro

import android.app.Application


class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		
		sDebugFetch = isDebugEnabled
	}
}
