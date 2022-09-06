package com.lhwdev.selfTestMacro.repository

import com.lhwdev.fetch.http.BasicSession
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.PasswordResult
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.api.UserGroup
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.database.DbUserGroup
import com.lhwdev.selfTestMacro.tryAtMost
import kotlinx.serialization.Serializable
import java.net.CookieManager
import java.net.CookiePolicy


abstract class SelfTestManagerBase : SelfTestManager {
	override val api: SelfTestApi = object : SelfTestApiMain() {
		override val anonymousSession: Session
			get() = this@SelfTestManagerBase.anonymousSession
	}
	
	
	/// DB - api Interop: convert db model to api model
	
	
	private var anonymousSession: Session = BasicSession()
	
	private val sessions = mutableMapOf<UserInfoKey, SelfTestApi.AuthorizedSession>()
	private val usersCache = mutableMapOf<UserInfoKey, User>()
	
	
	@Serializable
	protected data class UserInfoKey(val instituteCode: String, val userCode: String) {
		constructor(user: DbUser) : this(user.institute.code, user.userCode)
		constructor(user: User) : this(user.instituteCode, user.userCode)
	}
	
	
	override fun getAuthSession(group: DbUserGroup): SelfTestApi.AuthorizedSession = getAuthSession(
		institute = group.institute,
		userCode = with(database) { group.mainUser.userCode }
	)
	
	override fun registerAuthSession(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		mainUser: User
	) {
		sessions[UserInfoKey(institute.code, mainUser.userCode)] = session as SelfTestApi.AuthorizedSession
	}
	
	
	private fun createSession(): Session = anonymousSession.also {
		anonymousSession = BasicSession()
	}
	
	override fun createAuthSession(institute: InstituteInfo): SelfTestApi.AuthorizedSession {
		val previous = createSession()
		
		return SelfTestApi.AuthorizedSession(
			requestUrlBody = institute.requestUrlBody,
			cookieManager = previous.cookieManager ?: CookieManager(null, CookiePolicy.ACCEPT_ALL)
		)
	}
	
	private fun getAuthSession(
		institute: InstituteInfo,
		userCode: String
	) = sessions.getOrPut(UserInfoKey(institute.code, userCode)) {
		createAuthSession(institute)
	}
	
	private suspend fun authorizeSession(session: SelfTestApi.AuthorizedSession, group: DbUserGroup) =
		if(session.token == null) {
			val result = tryAtMost(maxTrial = 3) {
				api.validatePassword(
					session = session,
					institute = group.institute,
					token = group.usersIdentifier.token, password = group.password
				)
			}
			if(result is PasswordResult.Success) {
				session.token = result.token
				result.token
		} else {
			debugContext.onLightError(message = "password wrong?")
			null
		}
	} else {
		session.token
	}
	
	protected suspend fun ensureSessionAuthorized(group: DbUserGroup): Pair<SelfTestApi.AuthorizedSession, UserGroup.Token> {
		val session = getAuthSession(group)
		val token = authorizeSession(session, group)
		if(token == null) {
			val error = IllegalStateException("UserToken was not loaded")
			debugContext.onThrowError(message = "SelfTestManagerImpl: apiUser // 2", throwable = error)
			throw error
		}
		return session to token
	}
	
	protected suspend fun DbUser.apiUser(): User {
		val key = UserInfoKey(this)
		
		// 1. fast path #1
		usersCache[key]?.let { return it }
		
		// 2. slow path
		val group = with(database) { userGroup }
		
		val (session, token) = ensureSessionAuthorized(group)
		
		val users = api.getUserGroup(session, group.institute, token).users
		
		users.forEach {
			val userKey = UserInfoKey(it)
			usersCache[userKey] = it
		}
		return usersCache[key]
			?: error("UserGroup(= List<User>) for given DbUser $name did not contain User that matches key=$key")
	}
	
}
