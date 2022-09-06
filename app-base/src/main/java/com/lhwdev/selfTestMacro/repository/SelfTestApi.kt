package com.lhwdev.selfTestMacro.repository

import com.lhwdev.fetch.FetchBody
import com.lhwdev.fetch.FetchMethod
import com.lhwdev.fetch.FetchResult
import com.lhwdev.selfTestMacro.api.*
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL


interface SelfTestApi {
	/// Session management
	// TODO: persistence
	class AuthorizedSession(
		override val requestUrlBody: String,
		override val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL),
		override var keepAlive: Boolean? = null
	) : SelfTestManager.SelfTestSession {
		override var clientVersion: String = ""
		
		var token: UserGroup.Token? = null
		
		override suspend fun fetch(
			url: URL,
			method: FetchMethod?,
			headers: Map<String, String>,
			body: FetchBody?
		): FetchResult = super.fetch(url, method, headers, body).also {
			val clientVersion = it["X-Client-Version"]
			if(clientVersion != null) {
				this.clientVersion = clientVersion
			}
		}
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
		token: UserGroup.Token
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
