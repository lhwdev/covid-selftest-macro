@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.queryUrlParamsToString
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.sCommonUrl
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
public data class SearchResult(
	@SerialName("schulList")
	public val instituteList: List<InstituteInfo>,
	
	@SerialName("key")
	public val searchKey: SearchKey
)


// 학교: lctnScCode=03&schulCrseScCode=4&orgName=...&loginType=school
public suspend fun Session.searchSchool(
	regionCode: String?,
	schoolLevelCode: String,
	name: String
): SearchResult {
	var params = arrayOf(
		"schulCrseScCode" to schoolLevelCode,
		"orgName" to name,
		"loginType" to "school"
	)
	if(regionCode != null) params += "lctnScCode" to regionCode
	
	return fetch(
		url = sCommonUrl.get("searchSchool", *params),
		headers = sDefaultFakeHeader
	).toJsonLoose(SearchResult.serializer())
}

// 대학: orgName=...&loginType=univ
public suspend fun Session.searchUniversity(
	name: String
): List<InstituteInfo> {
	val params = queryUrlParamsToString(
		"orgName" to name,
		"loginType" to "univ"
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], headers = sDefaultFakeHeader)
		.toJsonLoose(SearchResult.serializer()).instituteList
}

// 교육행정기관: orgName=...&loginType=office
public suspend fun Session.searchOffice(
	name: String
): List<InstituteInfo> {
	val params = queryUrlParamsToString(
		"orgName" to name,
		"loginType" to "office"
	)
	
	return fetch(url = sCommonUrl["searchSchool?$params"], headers = sDefaultFakeHeader)
		.toJsonLoose(SearchResult.serializer()).instituteList
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
public suspend fun Session.searchAcademy(
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
	
	return fetch(url = sCommonUrl["searchSchool?$params"], headers = sDefaultFakeHeader)
		.toJsonLoose(SearchResult.serializer()).instituteList
}

