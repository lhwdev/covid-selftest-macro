package com.lhwdev.selfTestMacro

import android.content.Context
import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpInterceptor
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.github.repo.Repository
import com.lhwdev.github.sGithubInstanceDefault
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.ui.utils.sDebugAnimateListAsComposable
import java.net.URL
import javax.net.ssl.SSLHandshakeException


object FirstInitialization {
	init {
		sFetchInterceptors.addFirst(SelfTestHttpErrorRetryInterceptor)
	}
	
	fun Context.initializeApplication(
		versionName: String,
		versionCode: Int,
		flavor: String,
		debug: Boolean
	) {
		AppInitializationInfo.versionCode = versionCode
		AppInitializationInfo.versionName = versionName
		AppInitializationInfo.githubRepo = Repository(sGithubInstanceDefault, "lhwdev", "covid-selftest-macro")
		AppInitializationInfo.flavor = flavor
		AppInitializationInfo.debug = debug
		
		val pref = preferenceState
		
		if(pref.isDebugEnabled) {
			sDebugFetch = pref.isDebugFetchEnabled
			sDebugAnimateListAsComposable = pref.isNavigationDebugEnabled
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
