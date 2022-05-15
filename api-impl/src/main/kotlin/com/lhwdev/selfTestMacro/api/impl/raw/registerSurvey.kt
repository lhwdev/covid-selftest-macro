@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.api.AnswersMap
import com.lhwdev.selfTestMacro.api.InternalHcsApi
import com.lhwdev.selfTestMacro.api.Question
import com.lhwdev.selfTestMacro.api.UnstableHcsApi
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
	token: String,
	name: String,
	answer: AnswersMap,
	deviceUuid: String = ""
): SurveyResult = fetch(
	requestUrl["/registerServey"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to token
	),
	body = Bodies.jsonObject {
		"rspns00" set answer.isHealthy
		
		"rspns01" set if(answer.suspicious) "2" else "1"
		
		"rspns02" setNullable "1".takeIf { answer.quickTest == Question.QuickTest.Data.didNotConduct }
		
		"rspns07" setNullable when(answer.quickTest) {
			Question.QuickTest.Data.didNotConduct -> null
			Question.QuickTest.Data.negative -> "0"
			Question.QuickTest.Data.positive -> "1"
		}
		
		"rspns02" set if(answer.waitingResult) "0" else "1"
		
		"upperToken" set token
		"upperUserNameEncpt" set name
		"clientVersion" set clientVersion
		"deviceUuid" set deviceUuid
	}
).toJsonLoose(SurveyResult.serializer())
