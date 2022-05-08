@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose


public suspend fun Session.getUserInfo(institute: InstituteInfo, user: UserData): UserInfo = fetch(
	institute.requestUrl["/v2/getUserInfo"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to user.token.token
	),
	body = Bodies.jsonObject {
		"orgCode" set institute.code
		"userPNo" set user.userCode
	}
).toJsonLoose(UserInfo.serializer())
