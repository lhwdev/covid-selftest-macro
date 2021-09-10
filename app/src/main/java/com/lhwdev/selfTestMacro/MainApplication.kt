package com.lhwdev.selfTestMacro

import android.app.Application
import javax.net.ssl.SSLHandshakeException


class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		
		sDebugFetch = isDebugEnabled
		
		val last = sFetchInterceptors.first()
		sFetchInterceptors.add(0) { url, fetchMethod, map, session, fetchBody ->
			tryAtMost(maxTrial = 10, errorFilter = { it is SSLHandshakeException }) {
				last.intercept(url, fetchMethod, map, session, fetchBody)
			}
		}
	}
}
