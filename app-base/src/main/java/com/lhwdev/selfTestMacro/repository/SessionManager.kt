// package com.lhwdev.selfTestMacro.repository
//
// import com.lhwdev.fetch.http.Session
// import com.lhwdev.selfTestMacro.api.BasicHcsSession
// import net.gotev.cookiestore.InMemoryCookieStore
// import java.net.CookieManager
// import java.net.CookiePolicy
//
//
// object SessionManager {
// 	class SessionInfo(
// 		val session: Session,
// 		var anonymous: Boolean,
// 		var debugInfo: Any? = null,
// 		var sessionFullyLoaded: Boolean = false
// 	)
//	
// 	private val sessionMap = mutableMapOf<Any?, SessionInfo>()
//	
// 	private fun newHcsSession(name: Any?) = BasicHcsSession(
// 		CookieManager(
// 			InMemoryCookieStore(name = "selfTest/$name"),
// 			CookiePolicy.ACCEPT_ALL
// 		)
// 	)
//	
//	
// 	fun sessionFor(key: Any?): Session = sessionInfoFor(key = key).session
//	
// 	fun sessionInfoFor(key: Any?): SessionInfo {
// 		// 1. existing
// 		val existingInfo = sessionMap[key]
// 		if(existingInfo != null) return existingInfo
//		
// 		// 2. anonymous
// 		val anonymous = anonymousSessionInfoOrNull
// 		if(anonymous != null) {
// 			anonymousSessionInfoOrNull = null
// 			anonymous.anonymous = false
// 			anonymous.debugInfo = key
// 			sessionMap[key] = anonymous
// 			return anonymous
// 		}
//		
// 		val info = SessionInfo(session = newSession(key), anonymous = false)
// 		sessionMap[key] = info
// 		return info
// 	}
//	
// 	fun newDetachedSessionInfo(): SessionInfo {
// 		return SessionInfo(session = newSession("?"), anonymous = true)
// 	}
//	
// 	fun attachSessionInfo(info: SessionInfo, key: Any?) {
// 		info.anonymous = false
// 		info.debugInfo = key
// 		sessionMap[key] = info
// 	}
//	
// 	var anonymousSessionInfoOrNull: SessionInfo? = null
// 		private set
//	
// 	val anonymousSessionInfo: SessionInfo
// 		get() = anonymousSessionInfoOrNull ?: run {
// 			val info = SessionInfo(session = newSession("anonymous"), anonymous = true)
// 			anonymousSessionInfoOrNull = info
// 			info
// 		}
//	
// 	val anonymousSession: Session get() = anonymousSessionInfo.session
// 	val anySession: Session get() = sessionMap.values.firstOrNull()?.session ?: anonymousSessionInfo.session
// }
