package com.lhwdev.selfTestMacro.repository

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.AppDatabase
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.database.DbUserGroup
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.ui.UiContext


val LocalSelfTestManager = compositionLocalOf<SelfTestManager> { error("not provided") }


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
	interface SelfTestSession : HcsSession
	
	
	var context: Context
	val database: AppDatabase
	var debugContext: DebugContext
	
	val schedules: SelfTestSchedules
	
	val api: SelfTestApi
	
	fun createAuthSession(institute: InstituteInfo): SelfTestSession
	
	fun registerAuthSession(session: SelfTestSession, institute: InstituteInfo, mainUser: User)
	
	fun getAuthSession(group: DbUserGroup): SelfTestSession
	
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
	
	suspend fun onSubmitSchedule(schedule: SelfTestSchedule)
	
	fun updateSchedule(target: DbTestGroup, new: DbTestGroup)
	fun onScheduleUpdated()
}
