package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.api.*
import java.net.CookieManager
import java.net.CookiePolicy


interface SelfTestApi {
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
	
	suspend fun findSchool(
		regionCode: String?,
		schoolLevelCode: Int,
		name: String
	): SearchResult
	
	suspend fun findUser(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		name: String,
		birthday: String,
		searchKey: SearchKey,
		loginType: LoginType
	): UsersIdentifier
	
	suspend fun validatePassword(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		token: UsersIdToken,
		password: String
	): PasswordResult
	
	suspend fun getUserGroup(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		token: UsersToken
	): UserGroup
	
	suspend fun getUserInfo(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		user: User
	): UserInfo
	
	suspend fun registerSurvey(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		user: User,
		name: String,
		surveyData: SurveyData
	): SurveyResult
}
