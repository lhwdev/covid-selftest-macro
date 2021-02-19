@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer


@Serializable
data class User(
	@SerialName("userNameEncpt") val name: String,
	@SerialName("userPNo") val userCode: String,
	@SerialName("token") val token: UserToken
)


suspend fun getUserGroup(institute: InstituteInfo, token: UsersToken): List<User> = ioTask {
	fetch(
		institute.requestUrl2["selectUserGroup"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to token.token),
		body = "{}"
	).toJsonLoose(ListSerializer(User.serializer()))
}
