@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*


public suspend fun Session.getUserInfo(institute: InstituteInfo, user: User): UserInfo = fetch(
	institute.requestUrl2["getUserInfo"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf(
		"Authorization" to user.token.token
	),
	body = HttpBodies.jsonObject {
		"orgCode" set institute.code
		"userPNo" set user.userCode
	}
).toJsonLoose(UserInfo.serializer())
