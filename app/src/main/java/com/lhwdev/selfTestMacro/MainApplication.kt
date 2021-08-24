package com.lhwdev.selfTestMacro

import android.app.Application
import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpInterceptor
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_OFF
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.runBlocking
import java.net.URL


@Suppress("unused")
class MainApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		sDebugFetch = isDebugEnabled
		
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
		
		FirstInitialization
	}
}


object FirstInitialization {
	init {
		sFetchInterceptors.addFirst(SelfTestHttpErrorRetryInterceptor)
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
		val next = interceptorChain.interceptNext(url, method, headers, session, body)
		
		
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
