package com.lhwdev.selfTestMacro

import android.content.Context
import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpInterceptor
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.debugCheck
import com.lhwdev.selfTestMacro.debug.logOutput
import com.lhwdev.selfTestMacro.debug.sIncludeLogcatInLog
import com.lhwdev.selfTestMacro.navigation.sDebugNavigation
import com.lhwdev.selfTestMacro.repository.sDebugScheduleEnabled
import com.lhwdev.selfTestMacro.ui.utils.sDebugAnimateListAsComposable
import java.io.File
import java.net.URL
import javax.net.ssl.SSLHandshakeException


object FirstInitialization {
	init {
		sFetchInterceptors.addFirst(SelfTestHttpErrorRetryInterceptor)
	}
	
	// ~~for me: please use DI~~
	fun Context.initializeApplication(
		versionName: String,
		versionCode: Int,
		flavor: String,
		debug: Boolean,
		appIconForeground: Int,
		appIcon: Int,
		mainActivity: Class<*>
	) {
		AppInitializationInfo.versionCode = versionCode
		AppInitializationInfo.versionName = versionName
		AppInitializationInfo.flavor = flavor
		AppInitializationInfo.debug = debug
		
		AppInitializationInfo.appIconForeground = appIconForeground
		AppInitializationInfo.appIcon = appIcon
		
		// As it errors in inspection mode
		AppInitializationInfo.debugLogDirectory = getExternalFilesDir(null) ?: File("")
		
		AppInitializationInfo.mainActivity = mainActivity
		
		AppInitializationInfo.github = defaultGithubDataModel(sDefaultRepository)
		
		AppInitializationInfo.initialized = true
		
		
		// after initialization
		val pref = preferenceState
		
		if(pref.isDebugEnabled) {
			debugCheck = pref.isDebugCheckEnabled
			sDebugFetch = pref.isDebugFetchEnabled
			sDebugNavigation = pref.isNavigationDebugEnabled
			sDebugScheduleEnabled = pref.isScheduleDebugEnabled
			sDebugAnimateListAsComposable = pref.isNavigationDebugEnabled
			sIncludeLogcatInLog = pref.includeLogcatInLog
			
			val repository = preferenceState.virtualServer
			if(repository != null) {
				AppInitializationInfo.github = defaultGithubDataModel(repository)
			}
		}
		if(App.debug && !App.debuggingWithIde) {
			logOutput = getExternalFilesDir(null)!!.bufferedWriter()
		}
	}
}


/**
 * hcs.eduro.go.kr(senhcs, dgehcs, ...) returns HTTP 519 if session cookie is not valid.
 */
object SelfTestHttpErrorRetryInterceptor : HttpInterceptor {
	override suspend fun intercept(
		url: URL,
		method: HttpMethod,
		headers: Map<String, String>,
		session: Session?,
		body: DataBody?,
		interceptorChain: InterceptorChain
	): FetchResult {
		val next = tryAtMost(maxTrial = 10, errorFilter = { it is SSLHandshakeException }) {
			interceptorChain.interceptNext(url, method, headers, session, body)
		}
		
		if("eduro.go.kr" !in url.path) return next
		
		if(next.responseCode == 591 && "Set-Cookie" in next) {
			return interceptorChain.interceptNext(url, method, headers, session, body)
		}
		
		if(next.responseCode == 592) {
			println("-------- HTTP 592 --------")
		}
		
		return next
	}
}
