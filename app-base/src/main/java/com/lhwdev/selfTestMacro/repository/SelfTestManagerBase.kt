package com.lhwdev.selfTestMacro.repository

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
	class MySession(
		override val requestUrlBody: String,
		override val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL),
		override var keepAlive: Boolean? = null
	) : SelfTestManager.SelfTestSession {
		override var clientVersion: String = ""
		
		var token: UsersToken? = null
	}
	
	private val sessions = mutableMapOf<UserInfoKey, MySession>()
	private val usersCache = mutableMapOf<UserInfoKey, User>()
	// private val apiLoginCache: MutableMap<UsersIdToken, UsersToken> = mutableMapOf()
	// private val apiUsersCache: MutableMap<UserInfoKey, User> = mutableMapOf()
	
	
	@Serializable
	protected data class UserInfoKey(val instituteCode: String, val userCode: String) {
		constructor(user: DbUser) : this(user.institute.code, user.userCode)
		constructor(user: User) : this(user.instituteCode, user.userCode)
	}
	
	protected fun getSession(
		institute: InstituteInfo,
		userCode: String
	) = sessions.getOrPut(UserInfoKey(institute.code, userCode)) {
		MySession(requestUrlBody = institute.requestUrlBody)
	}
	
	override fun getSession(group: DbUserGroup): MySession = getSession(
		institute = group.institute,
		userCode = with(database) { group.mainUser.userCode }
	)
	
	protected suspend fun loadSession(
		session: SelfTestManager.SelfTestSession,
		usersIdentifier: UsersIdentifier,
		password: String,
		institute: InstituteInfo
	): UsersToken? = if((session as MySession).token == null) {
		val result = tryAtMost(maxTrial = 3) {
			session.validatePassword(institute, usersIdentifier.token, password)
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
	
	private suspend fun loadSession(session: MySession, group: DbUserGroup) = loadSession(
		session = session,
		usersIdentifier = group.usersIdentifier,
		password = group.password,
		institute = group.institute
	)
	
	protected suspend fun ensureSessionLoaded(group: DbUserGroup): Pair<MySession, UsersToken> {
		val session = getSession(group)
		val token = loadSession(session, group)
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
		
		val (session, token) = ensureSessionLoaded(group)
		
		val users = session.getUserGroup(group.institute, token)
		
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
	): List<InstituteInfo> = SessionManager.anySession.getSchoolData(
		regionCode = regionCode,
		schoolLevelCode = "$schoolLevelCode",
		name = name
	)
	
	override suspend fun findUser(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		name: String,
		birthday: String,
		loginType: LoginType
	): UsersIdentifier = session.findUser(
		institute = institute,
		name = name,
		birthday = birthday,
		loginType = loginType
	)
	
	override suspend fun validatePassword(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		token: UsersIdToken,
		password: String
	): PasswordResult = session.validatePassword(
		institute = institute,
		token = token,
		password = password
	)
	
	override suspend fun getUserGroup(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		token: UsersToken
	): List<User> = session.getUserGroup(
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
