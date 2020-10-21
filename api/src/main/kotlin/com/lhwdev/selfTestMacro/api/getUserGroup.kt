@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UserInfo(
	@SerialName("userNameEncpt") val name: String,
	@SerialName("userPNo") val userId: String,
	@SerialName("token") val token: UserToken
)


suspend fun getUserGroup(schoolInfo: SchoolInfo, token: UserToken): List<UserInfo> = ioTask {
	fetch(
		schoolInfo.requestUrl.child("selectUserGroup"),
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to token.token),
		body = "{}"
	).toJsonLoose()
}
