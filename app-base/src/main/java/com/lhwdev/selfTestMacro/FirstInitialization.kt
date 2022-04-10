package com.lhwdev.selfTestMacro

import android.content.Context
import com.lhwdev.fetch.FetchResult
import com.lhwdev.fetch.InterceptorChain
import com.lhwdev.fetch.http.HttpInterceptor
import com.lhwdev.fetch.http.HttpRequest
import com.lhwdev.fetch.sDebugFetch
import com.lhwdev.fetch.sFetchInterceptors
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.debug.debugCheck
import com.lhwdev.selfTestMacro.debug.logOutput
import com.lhwdev.selfTestMacro.debug.sIncludeLogcatInLog
import com.lhwdev.selfTestMacro.navigation.sDebugNavigation
import com.lhwdev.selfTestMacro.repository.sDebugScheduleEnabled
import com.lhwdev.selfTestMacro.ui.utils.sDebugAnimateListAsComposable
import java.io.File
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


// TODO: will be discarded after migration HcsSession 
/**
 * hcs.eduro.go.kr(senhcs, dgehcs, ...) returns HTTP 519 if session cookie is not valid.
 */
object SelfTestHttpErrorRetryInterceptor : HttpInterceptor {
	override suspend fun intercept(request: HttpRequest, interceptorChain: InterceptorChain): FetchResult {
		val next = tryAtMost(maxTrial = 10, errorFilter = { it is SSLHandshakeException }) {
			interceptorChain.interceptNext(request)
		}
		
		if("eduro.go.kr" !in request.url.path) return next
		
		if(next.responseCode == 591 && "Set-Cookie" in next) {
			return interceptorChain.interceptNext(request)
		}
		
		if(next.responseCode == 592) {
			println("-------- HTTP 592 --------")
		}
		
		return next
	}
}
