package com.lhwdev.selfTestMacro.repository

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.debug.DebugContext
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


enum class SelfTestInitiator(val isFromUi: Boolean) {
	userClick(isFromUi = true),
	alarm(isFromUi = false)
}


interface SelfTestManager {
	var context: Context
	val database: DatabaseManager
	val debugContext: DebugContext
	
	suspend fun createSession(): TempSession
	
	suspend fun findSchool(
		regionCode: String?,
		schoolLevelCode: Int,
		name: String
	): List<InstituteInfo>
	
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
	
	suspend fun getUserGroup(
		session: Session,
		institute: InstituteInfo,
		token: UsersToken
	): List<User>
	
	suspend fun getUserInfo(
		session: Session,
		institute: InstituteInfo,
		user: User
	): UserInfo
	
	fun addTestGroupToDb(usersToAdd: List<WizardUser>, targetGroup: DbTestGroup?, isAllGrouped: Boolean)
	
	
	suspend fun getCurrentStatus(user: DbUser): Status?
	
	
	/**
	 * Note: [users] may not be derived from database, rather arbitrary modified data to change answer etc.
	 */
	suspend fun submitSelfTestNow(
		uiContext: UiContext,
		group: DbTestGroup,
		users: List<DbUser>? = null
	): List<SubmitResult>
	
	fun updateSchedule(target: DbTestGroup, new: DbTestGroup)
	fun onScheduleUpdated()
}
