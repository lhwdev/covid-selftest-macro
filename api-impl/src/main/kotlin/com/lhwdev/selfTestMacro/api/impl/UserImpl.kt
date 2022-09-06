package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.api.impl.raw.*
import com.lhwdev.selfTestMacro.api.utils.LifecycleValue
import com.lhwdev.selfTestMacro.api.utils.getOrDefault
import com.lhwdev.selfTestMacro.utils.CachedSuspendState
import com.lhwdev.selfTestMacro.utils.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ex: 2022-05-15 20:34:33.467185
private val lastSurveyFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US)


@Suppress("MemberVisibilityCanBePrivate")
@OptIn(UnstableHcsApi::class, InternalHcsApi::class)
public class UserImpl @InternalHcsApi constructor(
	private var data: UserData,
	override val userGroup: UserGroup,
	override val institute: Institute,
	private val session: HcsSession,
	private var token: LifecycleValue<User.Token>
) : User {
	override val identifier: String get() = data.identifier
	
	override val name: String get() = data.name
	
	override val type: UserModel.Type get() = data.type
	
	override fun toData(): UserData = data
	
	
	private suspend fun getToken(): User.Token = token.getOrDefault body@{
		userGroup.
	}
	
	private val userInfo = CachedSuspendState {
		session.getUserInfo(data.institute.identifier, data.identifier, getToken())
	}
	
	public override val status: CachedSuspendState<UserModel.Status> = userInfo.map { value, refresh ->
		object : UserModel.Status {
			override val agreement: Boolean = value.agreement
			override val newNotificationCount: Int = value.newNoticeCount
			override val surveyResult = try {
				UserModel.Status.SurveyResult(
					answers = AnswersMap(
						Question.Suspicious to value.questionSuspicious!!,
						Question.QuickTest to value.questionQuickTestResult!!,
						Question.WaitingResult to value.questionWaitingResult!!
					),
					lastSurveyAt = lastSurveyFormat.parse(value.lastRegisterAt)
				)
			} catch(th: NullPointerException) {
				null
			}
			
			override suspend fun update(): HcsTemporaryModel = refresh()
		}
	}
	
	@OptIn(DangerousHcsApi::class)
	override suspend fun registerSurvey(answers: AnswersMap): UserModel.Status.SurveyResult {
		val apiResult = session.registerSurvey(
			token = getToken(),
			name = name,
			answers = answers,
			// deviceUuid = ""
		)
		val registerAt = try {
			lastSurveyFormat.parse(apiResult.registerAt)
		} catch(th: Throwable) {
			Date()
		}
		
		return UserModel.Status.SurveyResult(
			answers = answers,
			lastSurveyAt = registerAt
		)
	}
}


