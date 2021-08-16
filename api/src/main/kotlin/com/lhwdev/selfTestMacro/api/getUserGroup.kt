@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.fetch.jsonObject
import com.lhwdev.selfTestMacro.get
import com.lhwdev.selfTestMacro.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.builtins.ListSerializer


public suspend fun Session.getUserGroup(institute: InstituteInfo, token: UsersToken): List<User> = fetch(
	institute.requestUrl2["selectUserGroup"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
	body = Bodies.jsonObject {}
).toJsonLoose(ListSerializer(User.serializer()))
