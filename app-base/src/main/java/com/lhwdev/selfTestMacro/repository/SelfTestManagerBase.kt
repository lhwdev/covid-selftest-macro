package com.lhwdev.selfTestMacro.repository

import com.lhwdev.fetch.http.BasicSession
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.database.DbUserGroup
import com.lhwdev.selfTestMacro.tryAtMost
import kotlinx.serialization.Serializable
import java.net.CookieManager
import java.net.CookiePolicy


abstract class SelfTestManagerBase : SelfTestManager {
	/// DB - api Interop: convert db model to api model
	
	
	/// Session management
	// TODO: persistence
	class AuthorizedSession(
		override val requestUrlBody: String,
		override val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL),
		override var keepAlive: Boolean? = null
	) : SelfTestManager.SelfTestSession {
		override var clientVersion: String = ""
		
		var token: UsersToken? = null
	}
	
	private var anonymousSession: Session = BasicSession()
	
	private val sessions = mutableMapOf<UserInfoKey, AuthorizedSession>()
	private val usersCache = mutableMapOf<UserInfoKey, User>()
	// private val apiLoginCache: MutableMap<UsersIdToken, UsersToken> = mutableMapOf()
	// private val apiUsersCache: MutableMap<UserInfoKey, User> = mutableMapOf()
	
	
	@Serializable
	protected data class UserInfoKey(val instituteCode: String, val userCode: String) {
		constructor(user: DbUser) : this(user.institute.code, user.userCode)
		constructor(user: User) : this(user.instituteCode, user.userCode)
	}
	
	
	override fun getAuthSession(group: DbUserGroup): AuthorizedSession = getAuthSession(
		institute = group.institute,
		userCode = with(database) { group.mainUser.userCode }
	)
	
	override fun registerAuthSession(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		mainUser: User
	) {
		sessions[UserInfoKey(institute.code, mainUser.userCode)] = session as AuthorizedSession
	}
	
	
	private fun createSession(): Session = anonymousSession.also {
		anonymousSession = BasicSession()
	}
	
	override fun createAuthSession(institute: InstituteInfo): AuthorizedSession {
		val previous = createSession()
		
		return AuthorizedSession(
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
	
	private suspend fun authorizeSession(session: AuthorizedSession, group: DbUserGroup) = if(session.token == null) {
		val result = tryAtMost(maxTrial = 3) {
			session.validatePassword(
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
	
	protected suspend fun ensureSessionAuthorized(group: DbUserGroup): Pair<AuthorizedSession, UsersToken> {
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
		
		val users = session.getUserGroup(group.institute, token).users
		
		users.forEach {
			val userKey = UserInfoKey(it)
			usersCache[userKey] = it
		}
		return usersCache[key]
			?: error("UserGroup(= List<User>) for given DbUser $name did not contain User that matches key=$key")
	}
	
	
	/// Mandatory Apis for Self Test
	
	override suspend fun findSchool(
		regionCode: String?,
		schoolLevelCode: Int,
		name: String
	): SearchResult = anonymousSession.searchSchool(
		regionCode = regionCode,
		schoolLevelCode = "$schoolLevelCode",
		name = name
	)
	
	override suspend fun findUser(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		name: String,
		birthday: String,
		searchKey: SearchKey,
		loginType: LoginType
	): UsersIdentifier = session.findUser(
		institute = institute,
		name = name,
		birthday = birthday,
		searchKey = searchKey,
		loginType = loginType
	)
	
	override suspend fun validatePassword(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		token: UsersIdToken,
		password: String
	): PasswordResult {
		val result = session.validatePassword(
			institute = institute,
			token = token,
			password = password
		)
		if(result is PasswordResult.Success) {
			session as AuthorizedSession
			session.token = result.token
		}
		return result
	}
	
	override suspend fun getUserGroup(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		token: UsersToken
	): UserGroup = session.getUserGroup(
		institute = institute,
		token = token
	)
	
	override suspend fun getUserInfo(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		user: User
	): UserInfo = session.getUserInfo(
		institute = institute,
		user = user
	)
	
}
