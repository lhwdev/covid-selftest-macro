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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL


@Serializable
data class InstituteInfoResponse(@SerialName("schulList") val instituteList: List<InstituteInfo>)

@Serializable
data class InstituteInfo(
	@SerialName("kraOrgNm") val name: String,
	@SerialName("orgCode") val code: String,
	@SerialName("addres") val address: String,
	@SerialName("atptOfcdcConctUrl") val requestUrlBody: String
) {
	val requestUrl get() = URL("https://$requestUrlBody/v2")
	val requestUrlBase get() = URL("https://$requestUrlBody")
}


// 학교: lctnScCode=03&schulCrseScCode=4&orgName=...&loginType=school
suspend fun Session.getSchoolData(
	regionCode: String,
	schoolLevelCode: String,
	name: String
): InstituteInfoResponse {
	val params = queryUrlParamsToString(
		mapOf(
			"lctnScCode" to regionCode,
			"schulCrseScCode" to schoolLevelCode,
			"orgName" to name,
			"loginType" to "school"
		)
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer())
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

