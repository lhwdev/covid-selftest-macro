@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UserId(
	@SerialName("userNameEncpt") val name: String,
	@SerialName("userPNo") val userId: String,
	@SerialName("token") val token: UserIdToken
)


suspend fun getUserGroup(institute: InstituteInfo, token: UserToken): List<UserId> = ioTask {
	fetch(
		institute.requestUrl["selectUserGroup"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to token.token),
		body = "{}"
	).toJsonLoose()
}
