@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer


@Serializable
public data class User(
	@SerialName("userPNo") val userCode: String,
	@SerialName("token") val token: UserToken
)


public suspend fun Session.getUserGroup(institute: InstituteInfo, token: UsersToken): List<User> = fetch(
	institute.requestUrl2["selectUserGroup"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
	body = HttpBodies.jsonObject {}
).toJsonLoose(ListSerializer(User.serializer()))
