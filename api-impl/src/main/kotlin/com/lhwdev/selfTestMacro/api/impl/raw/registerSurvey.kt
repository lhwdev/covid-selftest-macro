@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@InternalHcsApi
@Serializable
public data class SurveyResult(
	@SerialName("registerDtm") val registerAt: String
	// what is 'inveYmd'?
)


@InternalHcsApi
@DangerousHcsApi
@OptIn(UnstableHcsApi::class)
public suspend fun HcsSession.registerSurvey(
	token: User.Token,
	name: String,
	answers: AnswersMap,
	deviceUuid: String = ""
): SurveyResult = fetch(
	requestUrl["/registerServey"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to token.token
	),
	body = Bodies.jsonObject {
		"rspns00" set answers.isHealthy
		
		"rspns01" set if(answers.suspicious) "2" else "1"
		
		"rspns02" setNullable "1".takeIf { answers.quickTest == Question.QuickTest.Data.didNotConduct }
		
		"rspns07" setNullable when(answers.quickTest) {
			Question.QuickTest.Data.didNotConduct -> null
			Question.QuickTest.Data.negative -> "0"
			Question.QuickTest.Data.positive -> "1"
		}
		
		"rspns02" set if(answers.waitingResult) "0" else "1"
		
		"upperToken" set token.token
		"upperUserNameEncpt" set name
		"clientVersion" set clientVersion
		"deviceUuid" set deviceUuid
	}
).toJsonLoose(SurveyResult.serializer())
