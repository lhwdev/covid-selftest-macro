package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.getSystemService
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.isOk
import com.lhwdev.selfTestMacro.android.utils.activeNetworkCommon
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.DiagnosticObject
import com.lhwdev.selfTestMacro.debug.TraceItems
import com.lhwdev.selfTestMacro.replaced
import com.lhwdev.selfTestMacro.repository.ui.showSelfTestFailedDialog
import com.lhwdev.selfTestMacro.tryAtMost
import com.lhwdev.selfTestMacro.ui.UiContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.WeakHashMap


@PublishedApi
internal val sSelfTestManagerMap = WeakHashMap<Context, SelfTestManager>()

inline fun Context.defaultSelfTestManager(create: (Context) -> SelfTestManager): SelfTestManager {
	val context = applicationContext
	return sSelfTestManagerMap.getOrPut(context) { create(context) }
}

fun Context.createDefaultSelfTestManager(debugContext: DebugContext): SelfTestManagerImpl = SelfTestManagerImpl(
	context = applicationContext,
	database = preferenceState.db,
	debugContext = debugContext.childContext(hint = "SelfTestManager"),
	defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
)


private fun userInfoKeyHash(userCode: String, instituteCode: String) =
	userCode.hashCode() * 31 + instituteCode.hashCode()


private const val sPrefPrefix = "SelfTestManager"

/**
 * A manager implementation for all SelfTest-related operations like submitting, scheduling, notification, etc,
 * for more high level operation.
 * I want to remove UI related things here, but I do not have such a time to do that.
 *
 * This and some classes, like [GroupTaskScheduler], [SelfTestSchedule], [NotificationStatus], [SelfTestLog] seperate
 * concerns that used to be focused here.
 *
 * Nowadays [SelfTestManager] focuses on:
 *
 * - database management
 * - translating calls to api implementation like [Session.registerSurvey] (although it is merely wrapper so far)
 * - fluent and user-friendly error handling
 * - scheduling self test
 */
@TraceItems(requiredModifier = java.lang.reflect.Modifier.PUBLIC)
class SelfTestManagerImpl(
	override var context: Context,
	override val debugContext: DebugContext,
	override val database: DatabaseManager,
	val defaultCoroutineScope: CoroutineScope
) : SelfTestManager {
	private val schedule = object : SelfTestSchedule(
		context = context,
		holder = context.preferenceHolderOf("$sPrefPrefix-todayStatus"),
		database = database,
		debugContext = debugContext.childContext(hint = "schedule")
	) {
		override suspend fun onScheduledSubmitSelfTest(group: DbTestGroup, users: List<DbUser>?) {
			try {
				withContext(Dispatchers.IO) {
					submitBulkSelfTest(group, users, isFromUi = false)
				}
			} catch(th: Throwable) {
				debugContext.onError("자가진단을 실패했습니다?!", throwable = th)
			}
		}
	}
	
	private val notificationStatus = NotificationStatus(
		holder = context.preferenceHolderOf("$sPrefPrefix-notificationStatus")
	)
	
	
	init {
		defaultCoroutineScope.launch {
			val pref = withContext(Dispatchers.Main) { context.preferenceState }
			
			snapshotFlow {
				pref.db.testGroups
			}.collect {
				onScheduleUpdated()
			}
		}
	}
	
	
	/// Error handling
	private suspend fun <R> handleError(
		operationName: String,
		target: DiagnosticObject,
		isFromUi: Boolean,
		onError: (HcsAppError) -> R,
		operation: suspend () -> R
	): R {
		var lastCauses: Set<HcsAppError.ErrorCause>? = null
		var trials = 0
		val throwables = mutableListOf<Throwable>()
		
		try {
			while(true) try {
				trials++
				
				return operation()
			} catch(th: Throwable) {
				val causes = mutableSetOf<HcsAppError.ErrorCause>()
				val diagnostic = SelfTestDiagnosticInfo()
				var isSerious = false
				
				
				// Possible reason:
				// - internal API structure change
				// - hcs maintenance
				// - app bug
				// - network not available
				// - network not responsive
				// - VPN
				// - just luck
				val conn = context.getSystemService<ConnectivityManager>()!!
				
				val network = conn.activeNetworkCommon
				
				
				fun makeError(): HcsAppError {
					diagnostic.networkInfo = network?.copy()
					val error = HcsAppError(
						message = "$operationName 실패",
						isSerious = isSerious,
						causes = causes,
						target = target,
						diagnosticItem = diagnostic
					)
					
					debugContext.onError(
						message = error.message,
						throwable = error
					)
					
					return error
				}
				
				if(network?.isAvailable == true) {
					isSerious = true
					val fetchResult = fetch(url = "https://hcs.eduro.go.kr/")
					
					if(fetchResult.isOk) {
						// Possible reason:
						// - internal API structure change
						// - app bug
						// - just luck
						
						causes += listOf(HcsAppError.ErrorCause.appBug, HcsAppError.ErrorCause.apiChange)
					} else {
						diagnostic.hcsAccessible = false
						causes += HcsAppError.ErrorCause.hcsUnreachable
						// Possible reason:
						// - network not responsive
						// - VPN
						// - just luck
						
						val isNetworkResponsive = pingNetwork()
						
						if(isNetworkResponsive) {
							// Network responsive but cannot access hcs.eduro.go.kr
							
							// Possible reason:
							// - VPN -> decided not to do something
							// - just luck
							
							
							if(network.isVpn) {
								causes += HcsAppError.ErrorCause.vpn
							} else {
								causes += HcsAppError.ErrorCause.probableApiChange
								causes += HcsAppError.ErrorCause.probableAppBug
							}
						} else {
							// Network not responsive
							causes += HcsAppError.ErrorCause.unresponsiveNetwork
						}
					}
					
				} else {
					isSerious = isSerious && !isFromUi
					// network?.isAvailable != true
					causes += HcsAppError.ErrorCause.noNetwork
				}
				
				// Fails when tried too much
				if(trials >= 3) return onError(makeError())
				
				// Fails when repeated error happens
				if(lastCauses == causes) {
					causes += HcsAppError.ErrorCause.repeated
					return onError(makeError())
				}
				
				lastCauses = causes
			}
		} finally {
			if(trials != 0) try {
				// had any error
				val messages = throwables.map { it.toString() }.toSet()
				debugContext.onLightError(
					"[handleError] $operationName: " + messages.joinToString(),
					shortLog = true
				)
			} catch(th: Throwable) {
				// holy
			}
		}
	}
	
	
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
	
	private fun getSessionInfo(
		userCode: String,
		institute: InstituteInfo
	): SessionManager.SessionInfo {
		return SessionManager.sessionInfoFor(keyFor(userCode, institute.code))
	}
	
	private fun getSessionInfo(group: DbUserGroup) = getSessionInfo(
		userCode = with(database) { group.allUsers.first().userCode },
		institute = group.institute
	)
	
	private suspend fun loadSession(
		info: SessionManager.SessionInfo,
		usersIdentifier: UsersIdentifier,
		password: String,
		institute: InstituteInfo
	): UsersToken? {
		if(!info.sessionFullyLoaded) {
			val result = tryAtMost(maxTrial = 3) {
				info.session.validatePassword(institute, usersIdentifier.token, password)
			}
			if(result is UsersToken) {
				apiLoginCache[usersIdentifier.token] = result
				info.sessionFullyLoaded = true
				return result
			} else debugContext.onLightError(message = "password wrong?")
		}
		
		return apiLoginCache[usersIdentifier.token]
	}
	
	private suspend fun loadSession(info: SessionManager.SessionInfo, group: DbUserGroup) = loadSession(
		info = info,
		usersIdentifier = group.usersIdentifier,
		password = group.password,
		institute = group.institute
	)
	
	private suspend fun ensureSessionLoaded(group: DbUserGroup): Pair<Session, UsersToken> {
		val info = getSessionInfo(group)
		val token = loadSession(info, group)
		if(token == null) {
			val error = IllegalStateException("UserToken was not loaded")
			debugContext.onThrowError(message = "SelfTestManagerImpl: apiUser // 2", throwable = error)
			throw error
		}
		return info.session to token
	}
	
	override suspend fun createSession(): TempSession = object : TempSession {
		private val sessionInfo = SessionManager.newDetachedSessionInfo()
		override val session: Session get() = sessionInfo.session
		
		override fun register(userCode: String, instituteCode: String) {
			SessionManager.attachSessionInfo(sessionInfo, keyFor(userCode, instituteCode))
			sessionInfo.sessionFullyLoaded = true
		}
	}
	
	
	private suspend fun DbUser.apiUser(): User {
		val key = keyFor(this)
		
		// 1. fast path #1
		apiUsersCache[key]?.let { return it }
		
		// 2. slow path
		val group = with(database) { userGroup }
		
		val (session, token) = ensureSessionLoaded(group)
		
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
	): List<InstituteInfo> = SessionManager.anySession.getSchoolData(
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
					userGroupId = thisGroupId,
					answer = Answer(
						suspicious = false,
						waitingResult = false,
						quarantined = false,
						housemateInfected = false
					)
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
			
			val ids = previousTestGroups.groups.keys.toMutableList()
			val newTestGroups = testTargets.map { target ->
				val id = ids.nextTestGroupId()
				ids += id
				DbTestGroup(id = id, target = target)
			}.associateBy { it.id }
			
			database.testGroups = previousTestGroups.copy(
				groups = previousTestGroups.groups + newTestGroups,
				maxGroupGeneratedNameIndex = maxGroupGeneratedNameIndex
			)
		} else {
			// add to existing group
			val testGroups = database.testGroups.groups.toMutableMap()
			
			if(targetGroup.id !in testGroups) error("what the...?") // what the error
			
			val testTarget = targetGroup.target as DbTestTarget.Group
			val added = testTarget.copy(
				userIds = testTarget.userIds + newUsers.map { it.id }
			)
			
			testGroups[targetGroup.id] = targetGroup.copy(target = added)
			database.testGroups = database.testGroups.copy(groups = testGroups)
		}
	}
	
	/// Current status
	
	override suspend fun getCurrentStatus(user: DbUser): Status? = with(database) {
		try {
			val (session, _) = ensureSessionLoaded(user.userGroup)
			Status(session.getUserInfo(user.usersInstitute, user.apiUser()))
		} catch(th: Throwable) {
			debugContext.onError(
				message = "${user.name}의 현재 상태를 불러오지 못했어요.",
				throwable = th,
				diagnostics = listOf(user)
			)
			null
		}
	}
	
	
	/// Core operations
	
	/**
	 * 자가진단이 실행될 가능성 높이기
	 * - 와이파이를 통해 네트워크 연결에 실패할 경우 데이터로 재시도 (저희 집 와이파이가 이상해서 안된 적이 있답니다)
	 * - VPN에 연결되어있으면 자동 접속해제
	 * - 안되면 몇번 재시도
	 * - 앱을 켜면 알림이 실행되지 않았는 적이 있는지 여부 확인 및 버그 제보 제안
	 * - 백그라운드 업데이트(3.1.0에 구현 예정)
	 */
	private suspend fun DatabaseManager.submitSelfTest(user: DbUser, isFromUi: Boolean): SubmitResult {
		val group = user.userGroup
		val info = getSessionInfo(group)
		
		return handleError(
			operationName = "자가진단 제출",
			isFromUi = isFromUi,
			target = user,
			onError = { SubmitResult.Failed(user, it.causes, it.diagnosticItem) }
		) {
			val session = tryAtMost(maxTrial = 3) {
				loadSession(info, group)
				info.session
			}
			
			val answer = user.answer
			val surveyData = SurveyData(
				questionSuspicious = answer.suspicious,
				questionWaitingResult = answer.waitingResult,
				questionQuarantined = answer.quarantined,
				upperUserName = answer.message
			)
			
			@OptIn(DangerousHcsApi::class)
			val data = session.registerSurvey(
				institute = user.usersInstitute,
				user = user.apiUser(),
				name = user.name,
				surveyData = surveyData
			)
			
			SubmitResult.Success(user, data.registerAt)
		}
	}
	
	private suspend fun submitBulkSelfTest(
		group: DbTestGroup,
		users: List<DbUser>?,
		isFromUi: Boolean
	): List<SubmitResult> = transactDb {
		val allUsers = users ?: with(database) { group.target.allUsers }
		val results = mutableListOf<SubmitResult>()
		
		var lastProbableApiChange = false
		
		for(user in allUsers) {
			val result = database.submitSelfTest(user, isFromUi = true)
			
			if(result is SubmitResult.Failed) {
				var userSpecific = true
				for(cause in result.causes) when(cause) {
					// implementation or version problem
					HcsAppError.ErrorCause.apiChange,
						// probably network problem?
					HcsAppError.ErrorCause.hcsUnreachable,
						// network problems
					HcsAppError.ErrorCause.vpn,
					HcsAppError.ErrorCause.noNetwork,
					HcsAppError.ErrorCause.unresponsiveNetwork ->
						userSpecific = false
					
					HcsAppError.ErrorCause.probableApiChange -> if(lastProbableApiChange) {
						userSpecific = false
					} else {
						lastProbableApiChange = true
					}
					
					// unknown
					HcsAppError.ErrorCause.appBug,
					HcsAppError.ErrorCause.probableAppBug -> Unit
					
					// ignore flags
					HcsAppError.ErrorCause.repeated -> Unit
				}
				
				if(!userSpecific) {
					break
				}
			}
			
			results += result
		}
		
		afterSelfTestSubmit(group, users, results, isFromUi)
		results
	}
	
	override suspend fun submitSelfTestNow(
		uiContext: UiContext,
		group: DbTestGroup,
		users: List<DbUser>?
	): List<SubmitResult> {
		return try {
			val results = submitBulkSelfTest(group, users, isFromUi = true)
			
			if(results.isEmpty()) return results
			
			if(results.all { it is SubmitResult.Success }) uiContext.scope.launch {
				uiContext.showMessage(
					if(group.target is DbTestTarget.Group) "모두 자가진단을 완료했어요." else "자가진단을 완료했어요.",
					"확인"
				)
			} else {
				val size = users?.size ?: group.target.allUserIds.size
				uiContext.navigator.showSelfTestFailedDialog(results, terminated = results.size < size)
			}
			
			results
		} catch(th: Throwable) {
			emptyList()
		}
	}
	
	
	private fun afterSelfTestSubmit(
		group: DbTestGroup,
		users: List<DbUser>?,
		results: List<SubmitResult>,
		isFromUi: Boolean
	) {
		schedule.updateStatus(group, users, complete = true)
		
		if(!isFromUi) {
			TODO("update NotificationStatus")
		}
		
		TODO("update SelfTestLog")
	}
	
	
	/// Scheduling
	
	private val lastGroups = database.testGroups.groups
	
	
	
	private fun setSchedule(alarmManager: AlarmManager, target: DbTestGroup) {
		return
		TODO()
	}
	
	override fun updateSchedule(target: DbTestGroup, new: DbTestGroup) {
		val testGroups = database.testGroups
		
		// change testGroups -> preferenceState.cache updated -> snapshotFlow(see above) -> call onScheduleUpdated
		disableOnScheduleUpdated = true
		try {
			database.testGroups = testGroups.copy(groups = testGroups.groups.replaced(from = target.id, to = new))
		} finally {
			disableOnScheduleUpdated = false
		}
		
		// setSchedule(alarmManager, new)
	}
	
	private var disableOnScheduleUpdated: Boolean = false
	
	override fun onScheduleUpdated(): Unit = with(database) {
		if(disableOnScheduleUpdated) return
		
		val newGroups = database.testGroups.groups
		if(lastGroups == newGroups) return
		
		val added = newGroups - lastGroups.keys
		val removed = lastGroups - newGroups.keys
		
		val alarmManager = context.getSystemService<AlarmManager>()!!
		for(group in removed) {
			println("TODO")
			// TODO
		}
		
		for(group in added.values) {
			setSchedule(alarmManager, group)
		}
	}
}
