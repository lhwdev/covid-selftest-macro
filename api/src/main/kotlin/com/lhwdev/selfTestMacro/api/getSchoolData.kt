@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL


@Serializable
data class SchoolInfoResponse(@SerialName("schulList") val schoolList: List<SchoolInfo>)

@Serializable
data class SchoolInfo(
	@SerialName("kraOrgNm") val name: String,
	@SerialName("orgCode") val code: String,
	@SerialName("addres") val address: String,
	@SerialName("atptOfcdcConctUrl") val requestUrlBody: String
) {
	val requestUrl get() = URL("https://$requestUrlBody/v2")
	val requestUrlBase get() = URL("https://$requestUrlBody")
}


enum class LoginType { school /* I don't know more; TODO */ }


// lctnScCode=03&schulCrseScCode=4&orgName=...&loginType=school
suspend fun getSchoolData(
	regionCode: String,
	schoolLevelCode: String,
	name: String,
	loginType: LoginType,
	pageNo: Int = 1
) = ioTask {
	val params = queryUrlParamsToString(mapOf(
		"lctnScCode" to regionCode,
		"schulCrseScCode" to schoolLevelCode,
		"orgName" to name,
		"loginType" to loginType.name,
//		"currentPageNo" to pageNo.toString()
	))
	
	val url = URL("$sCommonUrl/searchSchool?$params")
	fetch(url = url, method = HttpMethod.get, headers = sDefaultFakeHeader)
		.toJsonLoose<SchoolInfoResponse>()
}
