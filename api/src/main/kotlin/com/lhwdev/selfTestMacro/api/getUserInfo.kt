@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
private data class GetUserInfoRequestBody(
	@SerialName("orgCode") val instituteCode: String,
	@SerialName("userPNo") val userCode: String
)

@Serializable
data class UserInfo(
	@SerialName("userName") val userName: String,
	
	@SerialName("orgCode") val instituteCode: String,
	@SerialName("orgName") val instituteName: String,
	@SerialName("insttClsfCode") val institutionClassifierCode: String,
	
	@SerialName("atptOfcdcConctUrl") val instituteRequestUrlBody: String,
	@SerialName("lctnScCode") val instituteRegionCode: String? = null,
	@SerialName("schulCrseScCode") val schoolLevelCode: String? = null,
	@SerialName("sigCode") val instituteSigCode: String? = null,
	
	@SerialName("registerYmd") val lastRegisterDate: String? = null,
	@SerialName("registerDtm") val lastRegisterAt: String? = null,
	@SerialName("isHealthy") val isHealthy: Boolean? = null,
	
	@SerialName("deviceUuid") val deviceUuid: String? = null
) {
	val instituteStub: InstituteInfo = InstituteInfo(
		name = instituteName,
		code = instituteCode,
		address = "???",
		requestUrlBody = instituteRequestUrlBody
	)
	
	// see getInstituteData.kt
	val instituteType: InstituteType = when { // TODO: needs verification
		institutionClassifierCode == "5" -> InstituteType.school
		institutionClassifierCode == "7" -> InstituteType.university
		instituteRegionCode != null && schoolLevelCode != null -> InstituteType.school
		instituteRegionCode != null && instituteSigCode != null -> InstituteType.academy
		else -> InstituteType.office
	}
	
	fun toUserInfoString() = "$userName($instituteName)"
	fun toLastRegisterInfoString() =
		"최근 자가진단: ${if(lastRegisterAt == null) "미참여" else ((if(isHealthy == true) "정상" else "유증상") + "($lastRegisterAt)")}"
}


/*
 * admnYn: "N"
 * atptOfcdcConctUrl: "dgehcs.eduro.go.kr"
 * deviceUuid: "3b..."
 * insttClsfCode: "5" # 5 = school, 7 = univ
 * isHealthy: true
 * lctnScCode: "03"
 * lockYn: "N"
 * mngrClassYn: "N"
 * mngrDeptYn: "N"
 * orgCode: "D????????"
 * orgName: "??고등학교"
 * pInfAgrmYn: "Y"
 * registerDtm: "2020-10-21 07:05:43.187088"
 * registerYmd: "20201021"
 * schulCrseScCode: "4"
 * stdntYn: "Y"
 * token: "Bearer ey....."
 * upperUserName: "홍길동"
 * userName: "홍길동"
 * userNameEncpt: "홍길동"
 * userPNo: "..."
 * wrongPassCnt: 0
 */
suspend fun Session.getUserInfo(institute: InstituteInfo, user: User): UserInfo = fetch(
	institute.requestUrl2["getUserInfo"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Content-Type" to ContentTypes.json,
		"Authorization" to user.token.token
	),
	body = Json.encodeToString(
		GetUserInfoRequestBody.serializer(),
		GetUserInfoRequestBody(institute.code, user.userCode)
	)
).toJsonLoose(UserInfo.serializer())
