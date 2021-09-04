package com.lhwdev.selfTestMacro.repository

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.ui.UiContext


val LocalSelfTestManager = compositionLocalOf<SelfTestManager> { error("not provided") }


interface TempSession {
	val session: Session
	fun register(userCode: String, instituteCode: String)
}

@Immutable
data class MasterUser(
	val identifier: UsersIdentifier,
	val birth: String,
	val password: String,
	val instituteInfo: InstituteInfo,
	val instituteType: InstituteType
)

@Immutable
data class WizardUser(val user: User, val info: UserInfo, val master: MasterUser)


interface SelfTestManager {
	var context: Context
	
	suspend fun createSession(): TempSession
	suspend fun sessionFor(group: DbUserGroup): Session
	
	suspend fun findSchool(regionCode: String?, schoolLevelCode: Int, name: String): List<InstituteInfo>
	
	suspend fun findUser(
		session: Session,
		institute: InstituteInfo,
		name: String,
		birthday: String,
		loginType: LoginType
	): UsersIdentifier
	
	suspend fun validatePassword(
		session: Session,
		institute: InstituteInfo,
		token: UsersIdToken,
		password: String
	): PasswordResult
	
	suspend fun getUserGroup(session: Session, institute: InstituteInfo, token: UsersToken): List<User>
	
	suspend fun getUserInfo(session: Session, institute: InstituteInfo, user: User): UserInfo
	
	fun addTestGroupToDb(usersToAdd: List<WizardUser>, targetGroup: DbTestGroup?, isAllGrouped: Boolean)
	
	
	suspend fun getCurrentStatus(user: DbUser): Status?
	
	
	suspend fun submitSelfTestNow(
		context: UiContext,
		target: DbTestTarget,
		surveyData: (DbUser) -> SurveyData
	): List<SubmitResult>
	
	fun updateSchedule(target: DbTestGroup, schedule: DbTestSchedule)
	fun onScheduleUpdated()
}
