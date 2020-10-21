package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
private data class GetDetailedUserInfoRequestBody(
	@SerialName("orgCode") val schoolCode: String,
	@SerialName("userPNo") val userId: String
)

@Serializable
data class DetailedUserInfo(
	@SerialName("userInfo") val userName: String,
	@SerialName("registerYmd") val lastRegisterDate: String,
	@SerialName("registerDtm") val lastRegisterAt: String,
	@SerialName("isHealthy") val isHealthy: Boolean,
	@SerialName("deviceUuid") val deviceUuid: String?,
)


/*
 * admnYn: "N"
 * atptOfcdcConctUrl: "dgehcs.eduro.go.kr"
 * deviceUuid: "3b..."
 * insttClsfCode: "5"
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
suspend fun getDetailedUserInfo(schoolInfo: SchoolInfo, userInfo: UserInfo): DetailedUserInfo = ioTask {
	fetch(
		schoolInfo.requestUrl.child("getUserInfo"),
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to userInfo.token.token),
		body = Json.encodeToString(GetDetailedUserInfoRequestBody.serializer(),
			GetDetailedUserInfoRequestBody(schoolInfo.code, userInfo.userId))
	).toJsonLoose()
}
