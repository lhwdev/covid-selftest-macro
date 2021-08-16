@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.fetch.json
import com.lhwdev.selfTestMacro.get
import com.lhwdev.selfTestMacro.sDefaultFakeHeader
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
 * @param question1 `학생 본인이 37.5도 이상 발열 또는 발열감이 있나요?`
 * @param question2 `학생에게 코로나19가 의심되는 임상증상이 있나요? (기침, 호흡곤란, 오한, 근육통, 두통, 인후통, 후각·미각 소실 또는 폐렴 등)`
 * @param question3 `학생 본인 또는 동거인이 방역당국에 의해 현재 자가격리가 이루어지고 있나요?`
 */
public fun ActualSurveyData(
	deviceUuid: String,
	question1: Boolean = false,
	question2: Boolean = false,
	question3: Boolean = false,
	upperUserToken: UserToken,
	upperUserName: String
): SurveyData = SurveyData(
	deviceUuid = deviceUuid,
	rspns00 = !question1 && !question2 && !question3,
	rspns01 = if(question1) "2" else "1",
	rspns02 = if(question2) "0" else "1",
	rspns09 = if(question3) "1" else "0",
	upperUserToken = upperUserToken,
	upperUserName = upperUserName
)

@Serializable
public data class SurveyData(
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
	surveyData: SurveyData
): SurveyResult = fetch(
	institute.requestUrl["registerServey"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to user.token.token
	),
	body = Bodies.json(SurveyData.serializer(), surveyData, json = JsonEncodeDefaults)
).toJsonLoose(SurveyResult.serializer())
