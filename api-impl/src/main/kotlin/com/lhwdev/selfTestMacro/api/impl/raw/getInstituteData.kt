@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.get
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.api.InstituteData
import com.lhwdev.selfTestMacro.api.InstituteModel
import com.lhwdev.selfTestMacro.api.InternalHcsApi
import com.lhwdev.selfTestMacro.api.LifecycleValue
import com.lhwdev.selfTestMacro.sCommonUrl
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@InternalHcsApi
@Serializable
public class ApiSearchResult(
	@SerialName("schulList") public val instituteList: List<ApiInstituteInfo>,
	
	@SerialName("key") public val searchKey: String,
	
	@SerialName("sizeover") public val sizeover: Boolean
)


// TODO: pagination?
@InternalHcsApi
public suspend fun HcsSession.searchSchool(
	level: InstituteModel.School.Level,
	region: InstituteModel.School.Region?,
	name: String
): List<InstituteData.School> {
	var params = arrayOf(
		"schulCrseScCode" to "${level.code}",
		"orgName" to name,
		"loginType" to "school"
	)
	if(region != null) params += "lctnScCode" to region.code
	
	val result = fetch(
		url = sCommonUrl.get("searchSchool", *params),
		headers = sDefaultFakeHeader
	).toJsonLoose(ApiSearchResult.serializer())
	
	return result.instituteList.map { info ->
		InstituteData.School(
			identifier = info.vertificationCode,
			name = info.name,
			address = info.address,
			level = level,
			region = region ?: info.schoolRegion?.let { apiRegion ->
				InstituteModel.School.Region.values().find { it.code == apiRegion }
			} ?: error("could not find region for ${info.name}")
		).also {
			it.internalVerificationToken = with(LifecycleValue) {
				// searchKey is known to be expired in 2 minute
				InstituteData.InternalSearchKey(result.searchKey).expiresIn(millis = 2 * 60 * 1000)
			}
		}
	}
}

/*
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
*/

