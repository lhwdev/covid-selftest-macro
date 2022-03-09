@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.selfTestMacro.ContentTypes
import com.lhwdev.selfTestMacro.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer


@Serializable
data class User(
	@SerialName("userNameEncpt") val name: String,
	@SerialName("userPNo") val userId: String,
	@SerialName("token") val token: UserToken
)

// a temporary hack to pass clientVersion where proper api design is not done
class UserGroup(val users: List<User>, val clientVersion: String) : List<User> by users


suspend fun Session.getUserGroup(institute: InstituteInfo, token: UsersToken): UserGroup = fetch(
	institute.requestUrl["selectUserGroup"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to token.token),
	body = "{}"
).let {
	UserGroup(
		users = it.toJsonLoose(ListSerializer(User.serializer())),
		clientVersion = it.getHeader("X-Client-Version")
	)
}
