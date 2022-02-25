@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.selfTestMacro.ContentTypes
import com.lhwdev.selfTestMacro.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


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
 * Can be used only once.
 */
@Serializable(UserToken.Serializer::class)
data class UserToken(val token: String) {
	object Serializer : KSerializer<UserToken> {
		override val descriptor = PrimitiveSerialDescriptor(UserToken::class.java.name, PrimitiveKind.STRING)
		override fun deserialize(decoder: Decoder) = UserToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: UserToken) {
			encoder.encodeString(value.token)
		}
	}
}

@Serializable
data class SurveyData(
	val deviceUuid: String = "",
	@Serializable(with = YesNoSerializer::class) val rspns00: Boolean = true,
	val rspns01: String = "1",
	val rspns02: String = "1",
	val rspns03: String = "1",
	val rspns04: String? = null,
	val rspns05: String? = null,
	val rspns06: String? = null,
	val rspns07: String? = null,
	val rspns08: String = "0",
	val rspns09: String = "0",
	val rspns10: String? = null,
	val rspns11: String? = null,
	val rspns12: String? = null,
	val rspns13: String? = null,
	val rspns14: String? = null,
	val rspns15: String? = null,
	@SerialName("upperToken") val userToken: UserToken,
	@SerialName("upperUserNameEncpt") val upperUserName: String
)

@Serializable
data class SurveyResult(
	@SerialName("registerDtm") val registerAt: String
	// what is 'inveYmd'?
)

suspend fun Session.registerSurvey(
	institute: InstituteInfo,
	user: User,
	surveyData: SurveyData
): SurveyResult = fetch(
	institute.requestUrlBase["registerServey"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Content-Type" to ContentTypes.json,
		"Authorization" to user.token.token
	),
	body = JsonEncodeDefaults.encodeToString(SurveyData.serializer(), surveyData)
).toJsonLoose(SurveyResult.serializer())
