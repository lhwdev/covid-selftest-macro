@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
private data class InstituteInfoResponse(@SerialName("schulList") val instituteList: List<InstituteInfo>)


// 학교: lctnScCode=03&schulCrseScCode=4&orgName=...&loginType=school
public suspend fun Session.getSchoolData(
	regionCode: String,
	schoolLevelCode: String,
	name: String
): List<InstituteInfo> {
	val params = queryUrlParamsToString(
		"lctnScCode" to regionCode,
		"schulCrseScCode" to schoolLevelCode,
		"orgName" to name,
		"loginType" to "school"
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer()).instituteList
}

// 대학: orgName=...&loginType=univ
public suspend fun Session.getUniversityData(
	name: String
): List<InstituteInfo> {
	val params = queryUrlParamsToString(
		"orgName" to name,
		"loginType" to "univ"
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer()).instituteList
}

// 교육행정기관: orgName=...&loginType=office
public suspend fun Session.getOfficeData(
	name: String
): List<InstituteInfo> {
	val params = queryUrlParamsToString(
		"orgName" to name,
		"loginType" to "office"
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer()).instituteList
}

/*
 * [
 *   {
 *     "clsfCodeValue":"SIG_CODE",
 *     "cdcValue":"26110",
 *     "cdcValueNm":"중구",
 *     "cdcValueAbrvNm":"중구",
 *     "codeValueSeq":26110,
 *     "useYn":"Y",
 *     "userDfnCodeValue01":"26",
 *     "userDfnCodeValue02":"부산",
 *     "userDfnCodeValue03":"C",
 *     "upperClsfCodeValue":"LCTN_SC_CODE",
 *     "upperCdcValue":"02"
 *   },
 *   ...
 * ]
 */
@Serializable
public data class SigCode(
	@SerialName("cdcValueNm") val name: String,
	@SerialName("cdcValueAbrvNm") val nameAbbreviation: String,
	@SerialName("upperCdcValue") val sigCode: String
)

public suspend fun Session.getAvailableSigCodes(
	regionCode: String
): SigCode {
	val params = queryUrlParamsToString(
		"queryUrlParamsToString" to "SIG_CODE",
		"upperClsfCodeValue" to "LCTN_SC_CODE",
		"upperCdcValue" to regionCode,
		"stateKey" to "sigCodes"
	)
	return fetch(sCommonUrl["getMinors?$params"]).toJsonLoose(SigCode.serializer())
}

// 학원: lctnScCode=..&sigCode=....&orgName=...&isAcademySearch=true&loginType=office
public suspend fun Session.getAcademyData(
	regionCode: String,
	sigCode: String,
	name: String
): List<InstituteInfo> {
	val params = queryUrlParamsToString(
		"lctnScCode" to regionCode,
		"sigCode" to sigCode,
		"orgName" to name,
		"loginType" to "office",
		"isAcademySearch" to "true"
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose(InstituteInfoResponse.serializer()).instituteList
}

