package com.lhwdev.selfTestMacro.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.getSystemService
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.isOk
import com.lhwdev.selfTestMacro.android.utils.activeNetworkCommon
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.debug.*
import com.lhwdev.selfTestMacro.replaced
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


class HcsAppError(
	message: String,
	val isSerious: Boolean,
	val causes: Set<ErrorCause>,
	val target: Any? = null,
	val diagnosticItem: SelfTestDiagnosticInfo,
	cause: Throwable? = null
) : RuntimeException(null, cause), DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticItem
	private val mMessage = message
	private var cachedMessage: String? = null
	
	private fun createMessage() = buildString {
		append(mMessage)
		
		if(target != null) {
			append(" -> ")
			append(target)
		}
		
		append('\n')
		append("원인: ")
		causes.joinTo(this) { it.description }
		
		append("\n")
		append("진단 정보: ")
		diagnosticItem.dump(oneLine = true)
	}
	
	override val message: String
		get() = cachedMessage ?: run {
			val m = createMessage()
			cachedMessage = m
			m
		}
	
	enum class ErrorCause(
		val description: String,
		val detail: String?,
		val parent: ErrorCause? = null,
		val sure: ErrorCause? = null,
		val action: Action? = null,
		val category: SubmitResult.ErrorCategory
	) {
		repeated(description = "반복해서 일어난 오류", detail = null, category = SubmitResult.ErrorCategory.flag),
		
		noNetwork(
			description = "네트워크 연결 없음",
			detail = """
				네트워크에 연결되어 있지 않습니다.
				와이파이나 데이터 네트워크가 켜져있는지 확인하시고, 데이터만 켜져있을 경우 백그라운드 데이터 사용 제한을 해제하셨는지 확인해주세요.
			""".trimIndent(),
			action = Action.Notice("네트워크에 연결되어 있지 않아요."),
			category = SubmitResult.ErrorCategory.network
		),
		
		appBug(
			description = "앱 자체 버그",
			detail = """
				자가진단 매크로 앱의 버그입니다.
				오류정보를 복사해서 개발자에게 제보해주신다면 감사하겠습니다.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.bug
		),
		probableAppBug(
			description = "앱 자체 버그(?)",
			detail = """
				자가진단 매크로 앱의 버그일 수도 있습니다.
				만약 버그라고 생각되신다면 오류정보를 복사해서 개발자에게 제보해주신다면 감사하겠습니다.
			""".trimIndent(),
			sure = appBug,
			category = SubmitResult.ErrorCategory.bug
		),
		
		apiChange(
			description = "교육청 건강상태 자가진단의 내부 구조 변화",
			detail = """
				교육청의 건강상태 자가진단 사이트 내부구조가 바뀌었습니다.
				가능하다면 개발자에게 제보해주세요.
			""".trimIndent(),
			parent = appBug,
			category = SubmitResult.ErrorCategory.bug
		),
		probableApiChange(
			description = "교육청 건강상태 자가진단의 내부 구조 변화(?)",
			detail = """
				교육청의 건강상태 자가진단 사이트 내부구조가 바뀌었을 수 있습니다.
				버그인 것 같다면 개발자에게 제보해주세요.
			""".trimIndent(),
			sure = apiChange,
			category = SubmitResult.ErrorCategory.bug
		),
		
		unresponsiveNetwork(
			description = "네트워크 불안정",
			detail = """
				네트워크(와이파이, 데이터 네트워크 등)에 연결되어 있지만 인터넷에 연결할 수 없습니다.
				네트워크 연결을 다시 확인해주세요.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		),
		
		hcsUnreachable(
			description = "자가진단 사이트 접근 불가",
			detail = """
				네트워크에 연결되어 있고 인터넷에 연결할 수 있지만, 자가진단 사이트에 연결할 수 없습니다.
				교육청 건강상태 자가진단 서버가 순간적으로 불안정해서 일어났을 수도 있습니다.
				공식 자가진단 사이트나 앱에 들어가서 작동하는지 확인하고, 작동하는데도 이 에러가 뜬다면 버그를 제보해주세요.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		),
		
		vpn(
			description = "VPN 사용 중..?",
			detail = """
				VPN을 사용하고 있다면 VPN을 끄고 다시 시도해보세요.
				자가진단 서버는 해외에서 오는 연결을 싸그리 차단해버린답니다.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		);
		
		
		sealed class Action {
			class Notice(val message: String) : Action()
		}
	}
}


class SelfTestManagerImpl(
	override var context: Context,
	private val debugContext: DebugContext,
	private val database: DatabaseManager,
	val newAlarmIntent: (Context) -> Intent
) : SelfTestManager {
	/// Error handling
	private suspend fun <R> handleError(
		operationName: String,
		target: Any?,
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
						message = "[handleError] $operationName: throwing $error",
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
			} else context.onError(Throwable(), "password wrong?")
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
			context.onError(error, "SelfTestManagerImpl: apiUser // 2")
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
						quarantined = false
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
			val (session, _) = ensureSessionLoaded(user.userGroup)
			Status(session.getUserInfo(user.usersInstitute, user.apiUser()))
		} catch(th: Throwable) {
			selfLog("getCurrentStatus: error")
			th.printStackTrace()
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
	
	override suspend fun submitSelfTestNow(
		context: UiContext,
		target: DbTestTarget,
		initiator: SelfTestInitiator
	): List<SubmitResult> {
		return try {
			val result =
				with(database) { target.allUsers }.map { database.submitSelfTest(it, isFromUi = initiator.isFromUi) }
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
									
									Title { Text("오류 발생") }
									
									Content {
										resultItem.cause
									}
									
									Buttons {
										PositiveButton(onClick = requestClose) { Text("닫기") }
										Button(onClick = {}) {
											
										}
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
				
				Buttons { PositiveButton(onClick = requestClose) { Text("확인") } }
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
