@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.fetch.queryUrlParamsToString
import com.lhwdev.selfTestMacro.sCommonUrl
import com.lhwdev.selfTestMacro.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URL


@Serializable
class InstituteInfoResponse(
	@SerialName("key") val searchKey: InstituteSearchKey,
	@SerialName("schulList") val instituteList: List<InstituteInfo>
)

@Serializable
class InstituteInfosResult(
	val searchKey: InstituteSearchKey,
	val list: List<InstituteResult>
)

@Serializable(with = InstituteSearchKey.Serializer::class)
data class InstituteSearchKey(val key: String) {
	object Serializer : KSerializer<InstituteSearchKey> {
		override val descriptor = PrimitiveSerialDescriptor(InstituteSearchKey::class.java.name, PrimitiveKind.STRING)
		override fun deserialize(decoder: Decoder) = InstituteSearchKey(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: InstituteSearchKey) {
			encoder.encodeString(value.key)
		}
	}
}

@Serializable
data class InstituteInfo(
	@SerialName("kraOrgNm") val name: String,
	@SerialName("orgCode") val encryptedCode: String,
	@SerialName("juOrgCode") val persistentCode: String,
	
	@SerialName("addres") val address: String,
	@SerialName("atptOfcdcConctUrl") val requestUrlBody: String
) {
	val requestUrl get() = URL("https://$requestUrlBody")
}


// 학교: lctnScCode=03&schulCrseScCode=4&orgName=...&loginType=school
suspend fun Session.getSchoolData(
	regionCode: String,
	schoolLevelCode: String,
	name: String
): InstituteInfosResult {
	val params = queryUrlParamsToString(
		mapOf(
			"lctnScCode" to regionCode,
			"schulCrseScCode" to schoolLevelCode,
			"orgName" to name,
			"loginType" to "school"
		)
	)
	
	val result = fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer())
	return InstituteInfosResult(
		searchKey = result.searchKey,
		list = result.instituteList.map {
			InstituteResult(regionCode, schoolLevelCode, LoginType.school, it)
		}
	)
}

// 대학: orgName=...&loginType=univ
suspend fun Session.getUniversityData(
	name: String
): InstituteInfoResponse {
	val params = queryUrlParamsToString(
		mapOf("orgName" to name, "loginType" to "univ")
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer())
}

// 교육행정기관: orgName=...&loginType=office
suspend fun Session.getOfficeData(
	name: String
): InstituteInfoResponse {
	val params = queryUrlParamsToString(
		mapOf(
			"orgName" to name,
			"loginType" to "office"
		)
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer())
}

// 학원: lctnScCode=..&sigCode=....&orgName=...&isAcademySearch=true&loginType=office
suspend fun Session.getAcademyData(
	regionCode: String,
	sigCode: String,
	name: String
): InstituteInfoResponse {
	val params = queryUrlParamsToString(
		mapOf(
			"lctnScCode" to regionCode,
			"sigCode" to sigCode,
			"orgName" to name,
			"loginType" to "office",
			"isAcademySearch" to "true"
		)
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer())
}

