package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.getSystemService
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.onError
import com.lhwdev.selfTestMacro.replaced
import com.lhwdev.selfTestMacro.selfLog
import com.lhwdev.selfTestMacro.tryAtMost
import com.lhwdev.selfTestMacro.ui.Color
import com.lhwdev.selfTestMacro.ui.UiContext
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random


private fun Context.createScheduleIntent(
	id: Int, newAlarmIntent: (Context) -> Intent
): PendingIntent = PendingIntent.getBroadcast(
	this,
	id,
	newAlarmIntent(this),
	PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT >= 23) {
		PendingIntent.FLAG_IMMUTABLE
	} else {
		0
	}
)


private fun userInfoKeyHash(userCode: String, instituteCode: String) =
	userCode.hashCode() * 31 + instituteCode.hashCode()

class SelfTestManagerImpl(
	override var context: Context,
	private val database: DatabaseManager,
	val newAlarmIntent: (Context) -> Intent
) : SelfTestManager {
	
	/// Session management
	// TODO: persistence
	
	private val apiLoginCache: MutableMap<UsersIdToken, UsersToken> = mutableMapOf()
	private val apiUsersCache: MutableMap<UserInfoKey, User> = mutableMapOf()
	
	
	@Suppress("EqualsOrHashCode")
	private class UserInfoKey(val userCode: String, val instituteCode: String) {
		override fun hashCode(): Int = userInfoKeyHash(userCode, instituteCode)
	}
	
	private val keysCache = mutableListOf<UserInfoKey>()
	
	private fun keyFor(userCode: String, instituteCode: String): UserInfoKey {
		val hash = userInfoKeyHash(userCode, instituteCode)
		
		keysCache.find {
			it.hashCode() == hash && userCode == it.userCode && instituteCode == it.instituteCode
		}?.let { return it }
		
		val new = UserInfoKey(userCode, instituteCode)
		keysCache += new
		return new
	}
	
	private fun keyFor(user: DbUser) = keyFor(user.userCode, user.institute.code)
	private fun keyFor(user: User) = keyFor(user.userCode, user.instituteCode)
	
	suspend fun loadSession(
		usersIdentifier: UsersIdentifier,
		userCode: String,
		password: String,
		institute: InstituteInfo
	): Pair<Session, UsersToken?> {
		val info = SessionManager.sessionInfoFor(keyFor(userCode, institute.code))
		if(!info.sessionFullyLoaded) try {
			val result = tryAtMost(maxTrial = 2) {
				info.session.validatePassword(institute, usersIdentifier.token, password)
			}
			if(result is UsersToken) {
				apiLoginCache[usersIdentifier.token] = result
				info.sessionFullyLoaded = true
				return info.session to result
			} else onError(Throwable(), "password wrong?")
		} catch(th: Throwable) {
			onError(th, "MainRepositoryImpl.sessionFor")
		}
		
		return info.session to apiLoginCache[usersIdentifier.token]
	}
	
	suspend fun loadSession(group: DbUserGroup) = loadSession(
		group.usersIdentifier,
		with(database) { group.allUsers.first().userCode },
		group.password,
		group.institute
	)
	
	override suspend fun createSession(): TempSession = object : TempSession {
		private val sessionInfo = SessionManager.newDetachedSessionInfo()
		override val session: Session get() = sessionInfo.session
		
		override fun register(userCode: String, instituteCode: String) {
			SessionManager.attachSessionInfo(sessionInfo, keyFor(userCode, instituteCode))
			sessionInfo.sessionFullyLoaded = true
		}
	}
	
	override suspend fun sessionFor(group: DbUserGroup): Session = loadSession(group).first
	
	
	suspend fun DbUser.apiUser(): User {
		val key = keyFor(this)
		
		// 1. fast path #1
		apiUsersCache[key]?.let { return it }
		
		// 2. slow path
		val group = with(database) { userGroup }
		val (session, token) = loadSession(group)
		token!!
		
		val users = session.getUserGroup(group.institute, token)
		var result: User? = null
		users.forEach {
			val userKey = keyFor(it)
			apiUsersCache[userKey] = it
			if(userKey == key) result = it
		}
		return result ?: apiUsersCache[key]!!
	}
	
	
	/// Apis for wizard
	
	override suspend fun findSchool(
		regionCode: String?,
		schoolLevelCode: Int,
		name: String
	): List<InstituteInfo> = SessionManager.anonymousSession.getSchoolData(
		regionCode = regionCode,
		schoolLevelCode = "$schoolLevelCode",
		name = name
	)
	
	override suspend fun findUser(
		session: Session,
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
		session: Session,
		institute: InstituteInfo,
		token: UsersIdToken,
		password: String
	): PasswordResult = session.validatePassword(
		institute = institute,
		token = token,
		password = password
	)
	
	override suspend fun getUserGroup(
		session: Session,
		institute: InstituteInfo,
		token: UsersToken
	): List<User> = session.getUserGroup(
		institute = institute,
		token = token
	)
	
	override suspend fun getUserInfo(
		session: Session,
		institute: InstituteInfo,
		user: User
	): UserInfo = session.getUserInfo(
		institute = institute,
		user = user
	)
	
	override fun addTestGroupToDb(usersToAdd: List<WizardUser>, targetGroup: DbTestGroup?, isAllGrouped: Boolean) {
		// group by 'master user'
		val usersMap = usersToAdd.groupBy { it.master }
		
		val previousUserGroups = database.userGroups
		var userGroupId = previousUserGroups.maxId
		val newUserGroups = ArrayList<DbUserGroup>(usersMap.size)
		
		val previousUsers = database.users
		var userId = previousUsers.maxId
		val newUsers = ArrayList<DbUser>(usersToAdd.size)
		
		for((master, users) in usersMap) {
			val thisGroupId = ++userGroupId
			
			// user
			val dbUsers = users.map { user ->
				DbUser(
					id = ++userId,
					name = user.info.userName,
					userCode = user.user.userCode,
					userBirth = user.master.birth,
					institute = DbInstitute(
						type = user.info.instituteType,
						code = user.info.instituteCode,
						name = user.info.instituteName,
						classifierCode = user.info.instituteClassifierCode,
						hcsUrl = user.info.instituteRequestUrlBody,
						regionCode = user.info.instituteRegionCode,
						levelCode = user.info.schoolLevelCode,
						sigCode = user.info.instituteSigCode
					),
					userGroupId = thisGroupId
				)
			}
			newUsers += dbUsers
			
			// userGroup
			val dbGroup = DbUserGroup(
				id = thisGroupId,
				masterName = master.identifier.mainUserName,
				masterBirth = master.birth,
				password = master.password,
				userIds = dbUsers.map { it.id },
				usersIdentifier = master.identifier,
				instituteType = master.instituteType,
				institute = master.instituteInfo
			)
			newUserGroups += dbGroup
		}
		
		
		database.users = previousUsers.copy(
			users = previousUsers.users + newUsers.associateBy { it.id },
			maxId = userId
		)
		
		database.userGroups = previousUserGroups.copy(
			groups = previousUserGroups.groups + newUserGroups.associateBy { it.id },
			maxId = userGroupId
		)
		
		val previousTestGroups = database.testGroups
		val hadAnyTestGroups = previousTestGroups.groups.isNotEmpty()
		
		if(targetGroup == null) {
			// add new group
			var maxGroupGeneratedNameIndex =
				previousTestGroups.maxGroupGeneratedNameIndex
			
			// testGroup
			val testTargets = if(isAllGrouped) listOf(
				DbTestTarget.Group(
					"그룹 ${++maxGroupGeneratedNameIndex}",
					newUsers.map { it.id }
				)
			) else newUsers.map {
				DbTestTarget.Single(it.id)
			}
			
			val ids = previousTestGroups.ids
			val newTestGroups = testTargets.map { target ->
				val id = ids.nextTestGroupId()
				ids += id
				DbTestGroup(id = id, target = target)
			}
			
			database.testGroups = previousTestGroups.copy(
				groups = previousTestGroups.groups + newTestGroups,
				maxGroupGeneratedNameIndex = maxGroupGeneratedNameIndex
			)
		} else {
			// add to existing group
			val testGroups = database.testGroups.groups.toMutableList()
			
			val targetIndex = testGroups.indexOf(targetGroup)
			if(targetIndex == -1) error("what the...?") // what the error
			
			val testTarget = targetGroup.target as DbTestTarget.Group
			val added = testTarget.copy(
				userIds = testTarget.userIds + newUsers.map { it.id }
			)
			
			testGroups[targetIndex] = targetGroup.copy(target = added)
			database.testGroups = database.testGroups.copy(groups = testGroups)
		}
	}
	
	/// Current status
	
	override suspend fun getCurrentStatus(user: DbUser): Status? = with(database) {
		try {
			val session = sessionFor(user.userGroup)
			Status(session.getUserInfo(user.usersInstitute, user.apiUser()))
		} catch(th: Throwable) {
			selfLog("getCurrentStatus: error")
			th.printStackTrace()
			null
		}
	}
	
	
	/// Core operations
	
	@OptIn(DangerousHcsApi::class)
	private suspend fun DatabaseManager.submitSelfTest(
		target: DbTestTarget,
		surveyData: (DbUser) -> SurveyData
	): List<SubmitResult> = try {
		target.allUsers.map { user ->
			try {
				val session = sessionFor(user.userGroup)
				val data = session.registerSurvey(
					institute = user.usersInstitute,
					user = user.apiUser(),
					name = user.name,
					surveyData = surveyData(user)
				)
				SubmitResult.Success(user, data.registerAt)
			} catch(th: Throwable) {
				SubmitResult.Failed(user, "자가진단에 실패했어요.", th)
			}
		}
		
	} catch(th: Throwable) {
		emptyList()
	}
	
	
	override suspend fun submitSelfTestNow(
		context: UiContext,
		target: DbTestTarget,
		surveyData: (DbUser) -> SurveyData
	): List<SubmitResult> {
		return try {
			if(!context.context.isNetworkAvailable) {
				context.scope.launch {
					context.showMessage("네트워크에 연결되어 있지 않아요.", "확인")
				}
				return emptyList()
			}
			val result = database.submitSelfTest(target, surveyData)
			if(result.isEmpty()) return result
			
			if(result.all { it is SubmitResult.Success }) context.scope.launch {
				context.showMessage(
					if(target is DbTestTarget.Group) "모두 자가진단을 완료했어요." else "자가진단을 완료했어요.",
					"확인"
				)
			} else context.navigator.showDialogUnit {
				Title { Text("자가진단 실패") }
				
				ListContent {
					for(resultItem in result) when(resultItem) {
						is SubmitResult.Success -> ListItem {
							Text(
								"${resultItem.target.name}: 성공",
								color = Color(
									onLight = Color(0xf4259644),
									onDark = Color(0xff99ffa0)
								)
							)
						}
						
						is SubmitResult.Failed -> ListItem(
							modifier = Modifier.clickable {
								context.navigator.showDialogAsync {
									Title { Text("${resultItem.target.name} (${resultItem.target.institute.name}): ${resultItem.message}") }
									
									Content {
										val stackTrace = remember(resultItem.error) {
											resultItem.error.stackTraceToString()
										}
										Text(stackTrace)
									}
								}
							}
						) {
							Text(
								"${resultItem.target.name}: 실패",
								color = Color(
									onLight = Color(0xffff1122),
									onDark = Color(0xffff9099)
								)
							)
						}
					}
				}
				
				Buttons { PositiveButton { Text("확인") } }
			}
			result
		} catch(th: Throwable) {
			emptyList()
		}
	}
	
	
	/// Scheduling
	
	private val random = Random(System.currentTimeMillis())
	private val lastGroups = database.testGroups.groups
	private val intentCache = mutableMapOf<Int, PendingIntent>()
	
	private fun intentCache(id: Int) = intentCache.getOrPut(id) {
		context.createScheduleIntent(id, newAlarmIntent)
	}
	
	
	private fun DbTestGroup.nextTime(): Long {
		fun calendarFor(schedule: DbTestSchedule.Fixed): Calendar {
			val calendar = Calendar.getInstance()
			calendar[Calendar.SECOND] = 0
			calendar[Calendar.MILLISECOND] = 0
			
			calendar[Calendar.HOUR_OF_DAY] = schedule.hour
			calendar[Calendar.MINUTE] = schedule.minute
			return calendar
		}
		
		var calendar: Calendar? = null
		val timeInMillis = when(val schedule = schedule) {
			is DbTestSchedule.Fixed -> {
				calendar = calendarFor(schedule)
				calendar.timeInMillis
			}
			is DbTestSchedule.Random -> {
				val from = calendarFor(schedule.from).timeInMillis
				val to = calendarFor(schedule.to).timeInMillis
				random.nextLong(from = from, until = to + 1)
			}
			DbTestSchedule.None -> return -1L
		}
		
		if(excludeWeekend) {
			val c = if(calendar == null) {
				calendar = Calendar.getInstance()
				calendar.timeInMillis = timeInMillis
				calendar
			} else calendar
			
			while(true) {
				when(c[Calendar.DAY_OF_WEEK]) {
					Calendar.SATURDAY -> c.add(Calendar.DAY_OF_YEAR, 2)
					Calendar.SUNDAY -> c.add(Calendar.DAY_OF_YEAR, 1)
					else -> break
				}
			}
		}
		
		
		return timeInMillis
	}
	
	private fun setSchedule(alarmManager: AlarmManager, target: DbTestGroup) {
		val time = target.nextTime()
		
		if(time != -1L) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, intentCache(id = target.id))
		}
	}
	
	override fun updateSchedule(target: DbTestGroup, new: DbTestGroup) {
		val testGroups = database.testGroups
		
		val alarmManager = context.getSystemService<AlarmManager>()!!
		alarmManager.cancel(intentCache(target.id))
		
		
		// change testGroups -> preferenceState.cache updated -> state update -> snapshot mutation -> snapshotFlow(see ComposeApp.kt) -> call onScheduleUpdated
		disableOnScheduleUpdated = true
		database.testGroups = testGroups.copy(groups = testGroups.groups.replaced(from = target, to = new))
		disableOnScheduleUpdated = false
		
		setSchedule(alarmManager, new)
	}
	
	private var disableOnScheduleUpdated: Boolean = false
	
	override fun onScheduleUpdated(): Unit = with(database) {
		if(disableOnScheduleUpdated) return
		
		val newGroups = database.testGroups.groups
		if(lastGroups == newGroups) return
		
		val added = newGroups - lastGroups
		val removed = lastGroups - newGroups
		
		val alarmManager = context.getSystemService<AlarmManager>()!!
		for(group in removed) {
			val intent = intentCache(id = group.id)
			alarmManager.cancel(intent)
		}
		
		for(group in added) {
			setSchedule(alarmManager, group)
		}
	}
}
