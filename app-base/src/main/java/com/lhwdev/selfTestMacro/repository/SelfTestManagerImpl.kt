package com.lhwdev.selfTestMacro.repository

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.getSystemService
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.isOk
import com.lhwdev.selfTestMacro.android.utils.activeNetworkCommon
import com.lhwdev.selfTestMacro.api.QuickTestResult
import com.lhwdev.selfTestMacro.api.SurveyData
import com.lhwdev.selfTestMacro.api.registerSurvey
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.debug.DebugContext
import com.lhwdev.selfTestMacro.debug.DiagnosticObject
import com.lhwdev.selfTestMacro.debug.TraceItems
import com.lhwdev.selfTestMacro.replaced
import com.lhwdev.selfTestMacro.repository.ui.showSelfTestFailedDialog
import com.lhwdev.selfTestMacro.tryAtMost
import com.lhwdev.selfTestMacro.ui.UiContext
import com.lhwdev.utils.rethrowIfNeeded
import kotlinx.coroutines.*
import java.io.File
import java.util.WeakHashMap
import kotlin.time.Duration


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
	defaultCoroutineScope = CoroutineScope(Dispatchers.Default + Job())
)


private const val sPrefPrefix = "SelfTestManager"

/**
 * A manager implementation for all SelfTest-related operations like submitting, scheduling, notification, etc.,
 * for more high level operation.
 * I want to remove UI related things here, but I do not have such a time to do that.
 *
 * This and some classes, like [GroupTaskScheduler], [SelfTestSchedules], [NotificationStatus], [SelfTestLog] separate
 * concerns that used to be focused here.
 *
 * Nowadays [SelfTestManager] focuses on:
 *
 * - database management
 * - translating calls to api implementation like [Session.registerSurvey] (which is done by [api]: [SelfTestApi])
 * - fluent and user-friendly error handling
 * - scheduling self test
 */
@TraceItems(requiredModifier = java.lang.reflect.Modifier.PUBLIC)
class SelfTestManagerImpl(
	override var context: Context,
	override var debugContext: DebugContext,
	override val database: AppDatabase,
	val defaultCoroutineScope: CoroutineScope
) : SelfTestManagerBase() {
	override val schedules: SelfTestSchedulesImpl = object : SelfTestSchedulesImpl(
		context = context,
		holder = context.preferenceHolderOf("$sPrefPrefix-schedule"),
		database = database,
		debugContext = debugContext.childContext(hint = "schedule")
	) {
		override suspend fun onScheduledSubmitSelfTest(group: DbTestGroup, users: List<DbUser>?) {
			try {
				withContext(Dispatchers.IO) {
					submitBulkSelfTest(group, users, fromUi = false)
				}
			} catch(th: Throwable) {
				th.rethrowIfNeeded()
				debugContext.onError("자가진단을 실패했습니다?!", throwable = th)
			}
		}
	}
	
	private val notificationStatus = NotificationStatus(schedule = schedules, database = database, context = context)
	
	private val selfTestLog = SelfTestLog(
		logFile = File(context.getExternalFilesDir(null)!!, ""),
		holder = context.preferenceHolderOf("$sPrefPrefix-log"),
		coroutineScope = defaultCoroutineScope
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
		fromUi: Boolean,
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
				th.rethrowIfNeeded()
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
						diagnosticItem = diagnostic,
						cause = th
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
			if(trials > 1) try {
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
						sigCode = user.info.instituteSigCode
					),
					userGroupId = thisGroupId,
					answer = Answer(
						suspicious = false,
						quickTestResult = QuickTestResult.didNotConduct,
						waitingResult = false
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
			val (session, _) = ensureSessionAuthorized(user.userGroup)
			Status(api.getUserInfo(session, user.usersInstitute, user.apiUser()))
		} catch(th: Throwable) {
			th.rethrowIfNeeded()
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
	private suspend fun submitSelfTest(user: DbUser, fromUi: Boolean): SubmitResult = with(database) {
		val group = user.userGroup
		
		handleError(
			operationName = "자가진단 제출",
			fromUi = fromUi,
			target = user,
			onError = { SubmitResult.Failed(user, it.causes, it.diagnosticItem, it.cause) }
		) {
			val (session, _) = tryAtMost(maxTrial = 3) {
				ensureSessionAuthorized(group)
			}
			
			val answer = user.answer
			val surveyData = SurveyData(
				questionSuspicious = answer.suspicious,
				questionQuickTestResult = answer.quickTestResult,
				questionWaitingResult = answer.waitingResult,
				clientVersion = session.clientVersion,
				upperUserName = answer.message
			)
			
			val data = api.registerSurvey(
				session = session,
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
		fromUi: Boolean
	): List<SubmitResult> = transactDb { // - db synchronization, - log file(SelfTestLog) sync is deferred here
		val allUsers = users ?: with(database) { group.target.allUsers }
		val results = mutableListOf<SubmitResult>()
		
		var lastProbableApiChange = false
		
		for(user in allUsers) {
			val result = submitSelfTest(user, fromUi = fromUi)
			
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
		
		afterSelfTestSubmit(group, users, results, fromUi)
		results
	}
	
	override suspend fun submitSelfTestNow(
		uiContext: UiContext,
		group: DbTestGroup,
		users: List<DbUser>?
	): List<SubmitResult> {
		return try {
			val results = submitBulkSelfTest(group, users, fromUi = true)
			
			if(results.isEmpty()) return results
			
			if(results.all { it is SubmitResult.Success }) uiContext.scope.launch {
				uiContext.showMessage(
					if(group.target is DbTestTarget.Group) "모두 자가진단을 완료했어요." else "자가진단을 완료했어요.",
					"확인"
				)
			} else {
				val size = users?.size ?: group.target.allUsersCount
				uiContext.navigator.showSelfTestFailedDialog(results, terminated = results.size < size)
			}
			
			results
		} catch(th: Throwable) {
			th.rethrowIfNeeded()
			emptyList()
		}
	}
	
	override suspend fun onSubmitSchedule(schedule: SelfTestSchedule) {
		schedule as SelfTestSchedulesImpl.Schedule
		schedules.scheduler.onSchedule(schedule.schedule, defaultCoroutineScope)?.join()
	}
	
	
	private suspend fun afterSelfTestSubmit(
		group: DbTestGroup,
		users: List<DbUser>?,
		results: List<SubmitResult>,
		fromUi: Boolean
	) {
		val allUsers = users ?: with(database) { group.target.allUsers }
		
		// log entries
		val logEntryId = selfTestLog.nextId
		for(index in allUsers.indices) {
			val user = allUsers[index]
			val result = results.getOrNull(index) ?: continue
			
			selfTestLog.logSelfTest(
				database,
				user,
				success = result is SubmitResult.Success,
				message = when(result) {
					is SubmitResult.Success -> "자가진단 성공"
					is SubmitResult.Failed -> result.description
					
				}
			)
		}
		schedules.updateStatus(group, users, results, logRange = logEntryId until selfTestLog.nextId)
		
		notificationStatus.onStatusUpdated(fromUi = fromUi)
		notificationStatus.onSubmitSelfTest(allUsers, results)
	}
	
	
	/// Scheduling
	
	
	override fun updateSchedule(target: DbTestGroup, new: DbTestGroup) {
		val testGroups = database.testGroups
		database.testGroups = testGroups.copy(groups = testGroups.groups.replaced(from = target.id, to = new))
		// onScheduleUpdated is called by itself
	}
	
	override fun onScheduleUpdated() {
		schedules.onScheduleUpdated()
	}
}


fun Long.millisToDeltaString(now: Long = System.currentTimeMillis()): String =
	with(Duration) { (this@millisToDeltaString - now).milliseconds }.toString()
