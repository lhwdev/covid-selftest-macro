@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.fetch.json
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/*
 * deviceUuid: ""
 * rspns00: "Y"
 * rspns01: "1"
 * rspns02: "1"
 * rspns03: null
 * rspns04: null
 * rspns05: null
 * rspns06: null
 * rspns07: null
 * rspns08: null
 * rspns09: "0"
 * rspns10: null
 * rspns11: null
 * rspns12: null
 * rspns13: null
 * rspns14: null
 * rspns15: null
 * upperToken: "Bearer ey..."
 * upperUserNameEncpt: "홍길동"
 */


/**
 * @param questionSuspicious `본인이 코로나19가 의심되는 아래의 임상증상(발열 (37.5℃ 이상), 기침, 호흡곤란, 오한, 근육통, 두통, 인후통, 후각·미각 소실)이 있나요?`
 * @param questionWaitingResult `본인 또는 동거인이 코로나19 진단검사를 받고 그 결과를 기다리고 있나요?`
 * @param questionQuarantined `본인 또는 동거인이 방역당국에 의해 현재 자가격리가 이루어지고 있나요?`
 * @param questionHousemateInfected `4. 동거인 중 확진자가 있나요?`
 */
public data class SurveyData(
	val questionSuspicious: Boolean = false,
	val questionWaitingResult: Boolean = false,
	val questionQuarantined: Boolean = false,
	val questionHousemateInfected: Boolean = false,
	val deviceUuid: String = "",
	val upperUserToken: UserToken? = null, // null for default
	val upperUserName: String? = null // null for default
) {
	public fun toApiSurveyData(user: User, name: String): ApiSurveyData = ApiSurveyData(
		upperUserToken = user.token,
		upperUserName = name,
		rspns00 = !questionSuspicious && !questionWaitingResult && !questionQuarantined && !questionHousemateInfected,
		rspns01 = if(questionSuspicious) "2" else "1",
		rspns02 = if(questionWaitingResult) "0" else "1",
		rspns09 = if(questionQuarantined) "1" else "0",
		rspns08 = if(questionHousemateInfected) "1" else "0"
	)
}

@Serializable
public data class ApiSurveyData(
	val deviceUuid: String = "",
	@Serializable(with = YesNoSerializer::class) val rspns00: Boolean = true,
	val rspns01: String = "1",
	val rspns02: String = "1",
	val rspns03: String? = null,
	val rspns04: String? = null,
	val rspns05: String? = null,
	val rspns06: String? = null,
	val rspns07: String? = null,
	val rspns08: String? = null,
	val rspns09: String = "0",
	val rspns10: String? = null,
	val rspns11: String? = null,
	val rspns12: String? = null,
	val rspns13: String? = null,
	val rspns14: String? = null,
	val rspns15: String? = null,
	@SerialName("upperToken") val upperUserToken: UserToken,
	@SerialName("upperUserNameEncpt") val upperUserName: String
)

@Serializable
public data class SurveyResult(
	@SerialName("registerDtm") val registerAt: String
	// what is 'inveYmd'?
)


@DangerousHcsApi
public suspend fun Session.registerSurvey(
	institute: InstituteInfo,
	user: User,
	name: String,
	surveyData: SurveyData
): SurveyResult = fetch(
	institute.requestUrl["registerServey"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to user.token.token
	),
	body = Bodies.json(ApiSurveyData.serializer(), surveyData.toApiSurveyData(user, name), json = JsonEncodeDefaults)
).toJsonLoose(SurveyResult.serializer())
