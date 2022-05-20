package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.api.impl.raw.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ex: 2022-05-15 20:34:33.467185
private val lastSurveyFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US)


@Suppress("MemberVisibilityCanBePrivate")
@OptIn(UnstableHcsApi::class, InternalHcsApi::class)
public class UserImpl @InternalHcsApi constructor(
	public val data: UserData,
	override val institute: InstituteImpl,
	private val session: HcsSession,
	private val userToken: String
) : User, UserModel by data {
	private val userInfo = ExternalStateImpl<UserInfo> {
		session.getUserInfo(data.institute.identifier, data.identifier, userToken)
	}
	
	public override val status: ExternalState<UserModel.Status> get() = mStatus
	private val mStatus = userInfo.map<UserInfo, UserModel.Status> { value, updater ->
		object : UserModel.Status {
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
			
			override suspend fun update(): HcsTemporaryModel = updater()
		}
	}
	
	@OptIn(DangerousHcsApi::class)
	override suspend fun registerSurvey(answers: AnswersMap): UserModel.Status.SurveyResult {
		val apiResult = session.registerSurvey(
			token = userToken,
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


