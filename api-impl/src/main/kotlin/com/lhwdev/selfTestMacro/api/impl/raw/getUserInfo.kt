@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.api.InternalHcsApi
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.toJsonLoose


@InternalHcsApi
public suspend fun HcsSession.getUserInfo(
	instituteCode: String,
	userCode: String,
	token: User.Token
): ApiUserInfo = fetch(
	requestUrl["/v2/getUserInfo"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
	body = Bodies.jsonObject {
		"orgCode" set instituteCode
		"userPNo" set userCode
	}
).toJsonLoose(ApiUserInfo.serializer())
