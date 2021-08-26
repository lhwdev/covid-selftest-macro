package com.lhwdev.selfTestMacro

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.runBlocking


class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		
		sDebugFetch = isDebugEnabled
	}
}
