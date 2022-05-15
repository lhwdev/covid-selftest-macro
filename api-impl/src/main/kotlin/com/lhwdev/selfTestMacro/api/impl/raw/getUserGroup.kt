@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.api.InternalHcsApi
import com.lhwdev.selfTestMacro.api.impl.UserImpl
import com.lhwdev.selfTestMacro.toJsonLoose
import kotlinx.serialization.builtins.ListSerializer


@InternalHcsApi
public suspend fun HcsSession.getUserGroup(token: UsersToken): List<UserImpl> = fetch(
	requestUrl["/v2/selectUserGroup"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
	body = Bodies.jsonObject {}
).toJsonLoose(ListSerializer(UserImpl.serializer()))
