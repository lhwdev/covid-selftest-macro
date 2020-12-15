@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
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
suspend fun getSchoolData(
	regionCode: String,
	schoolLevelCode: String,
	name: String
) = ioTask {
	val params = queryUrlParamsToString(mapOf(
		"lctnScCode" to regionCode,
		"schulCrseScCode" to schoolLevelCode,
		"orgName" to name,
		"loginType" to "school"
	))
	
	fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose<InstituteInfoResponse>()
}

// 대학: orgName=...&loginType=univ
suspend fun getUniversityData(
	name: String
) = ioTask {
	val params = queryUrlParamsToString(mapOf(
		"orgName" to name,
		"loginType" to "univ"
	))
	
	fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose<InstituteInfoResponse>()
}

// 교육행정기관: orgName=...&loginType=office
suspend fun getOfficeData(
	name: String
) = ioTask {
	val params = queryUrlParamsToString(mapOf(
		"orgName" to name,
		"loginType" to "office"
	))
	
	fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose<InstituteInfoResponse>()
}
// 학원: lctnScCode=..&sigCode=....&orgName=...&isAcademySearch=true&loginType=office
suspend fun getAcademyData(
	regionCode: String,
	sigCode: String,
	name: String
) = ioTask {
	val params = queryUrlParamsToString(mapOf(
		"lctnScCode" to regionCode,
		"sigCode" to sigCode,
		"orgName" to name,
		"loginType" to "office",
		"isAcademySearch" to "true"
	))
	
	fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose<InstituteInfoResponse>()
}

