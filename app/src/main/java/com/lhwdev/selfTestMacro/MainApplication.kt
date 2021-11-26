package com.lhwdev.selfTestMacro

import android.app.Application
import com.lhwdev.fetch.sDebugFetch
import com.lhwdev.fetch.sFetchInterceptors
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
