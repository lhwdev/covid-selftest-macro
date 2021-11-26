@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer


@Serializable
data class User(
	@SerialName("userNameEncpt") val name: String,
	@SerialName("userPNo") val userId: String,
	@SerialName("token") val token: UserToken
)


suspend fun Session.getUserGroup(institute: InstituteInfo, token: UsersToken): List<User> = fetch(
	institute.requestUrl["selectUserGroup"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to token.token),
	body = "{}"
).toJsonLoose(ListSerializer(User.serializer()))
