@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


/*
 * deviceUuid: ""
 * rspns00: "Y
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



@Serializable(RegisterSurveyToken.Serializer::class)
data class RegisterSurveyToken(val token: String) {
	object Serializer : KSerializer<RegisterSurveyToken> {
		override val descriptor = PrimitiveSerialDescriptor(RegisterSurveyToken::class.java.name, PrimitiveKind.STRING)
		override fun deserialize(decoder: Decoder) = RegisterSurveyToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: RegisterSurveyToken) {
			encoder.encodeString(value.token)
		}
	}
}

@Serializable
data class SurveyData(
	val deviceUuid: String = "",
	val rspns00: String = "Y",
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
	@SerialName("upperToken") val userToken: String,
	@SerialName("upperUserNameEncpt") val userName: String
)

@Serializable
data class SurveyResult(
	@SerialName("registerDtm") val registerAt: String
	// what is 'inveYmd'?
)

suspend fun registerSurvey(
	institute: InstituteInfo,
	token: RegisterSurveyToken,
	surveyData: SurveyData
): SurveyResult = ioTask {
	fetch(
		institute.requestUrlBase["registerServey"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf(
			"Content-Type" to ContentTypes.json,
			"Authorization" to token.token
		),
		body = Json { encodeDefaults = true }.encodeToString(SurveyData.serializer(), surveyData)
	).toJsonLoose()
}
