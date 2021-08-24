package com.lhwdev.selfTestMacro.repository

import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.DbUserGroup
import net.gotev.cookiestore.InMemoryCookieStore
import java.net.CookieManager
import java.net.CookiePolicy


data class SessionUserKey(val name: String, val birth: String, val instituteCode: String)

fun SessionManager.sessionInfoFor(
	database: DatabaseManager,
	group: DbUserGroup
): SessionManager.SessionInfo =
	sessionInfoFor(SessionUserKey(group.masterName, group.masterBirth, group.institute.code))

fun SessionManager.sessionFor(database: DatabaseManager, group: DbUserGroup): Session =
	sessionInfoFor(database = database, group = group).session


object SessionManager {
	class SessionInfo(
		val session: Session,
		var anonymous: Boolean,
		var debugInfo: Any? = null,
		var sessionFullyLoaded: Boolean = false
	)
	
	private val sessionMap = mutableMapOf<Any?, SessionInfo>()
	
	private fun newSession(name: Any?) =
		Session(
			CookieManager(
				InMemoryCookieStore(name = "selfTest/$name"),
				CookiePolicy.ACCEPT_ALL
			)
		)
	
	
	fun sessionFor(key: Any?): Session = sessionInfoFor(key = key).session
	
	fun sessionInfoFor(key: Any?): SessionInfo {
		// 1. existing
		val existingInfo = sessionMap[key]
		if(existingInfo != null) return existingInfo
		
		// 2. anonymous
		val anonymous = anonymousSessionInfoOrNull
		if(anonymous != null) {
			anonymousSessionInfoOrNull = null
			anonymous.anonymous = false
			anonymous.debugInfo = key
			sessionMap[key] = anonymous
			return anonymous
		}
		
		val info = SessionInfo(session = newSession(key), anonymous = false)
		sessionMap[key] = info
		return info
	}
	
	var anonymousSessionInfoOrNull: SessionInfo? = null
		private set
	
	val anonymousSessionInfo: SessionInfo
		get() = anonymousSessionInfoOrNull ?: run {
			val info = SessionInfo(session = newSession("anonymous"), anonymous = true)
			anonymousSessionInfoOrNull = info
			info
		}
	
	val anonymousSession: Session get() = anonymousSessionInfo.session
}
