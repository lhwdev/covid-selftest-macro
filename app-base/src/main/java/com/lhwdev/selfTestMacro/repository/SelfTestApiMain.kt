package com.lhwdev.selfTestMacro.repository

import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*


abstract class SelfTestApiMain : SelfTestApi {
	abstract val anonymousSession: Session
	
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
			session as SelfTestApi.AuthorizedSession
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
	
	@OptIn(DangerousHcsApi::class)
	override suspend fun registerSurvey(
		session: SelfTestManager.SelfTestSession,
		institute: InstituteInfo,
		user: User,
		name: String,
		surveyData: SurveyData
	): SurveyResult = session.registerSurvey(institute, user, name, surveyData)
}
