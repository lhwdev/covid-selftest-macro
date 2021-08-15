@file:Suppress("SpellCheckingInspection", "INVISIBLE_REFERENCE")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*


// TODO: multiple users with same institute, name, birthday, loginType (that exists in hcs code)
public suspend fun Session.findUser(
	institute: InstituteInfo,
	name: String,
	birthday: String,
	loginType: LoginType,
	pageNumber: Int? = null
): UsersIdentifier = fetch(
	institute.requestUrl2["findUser"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader,
	body = HttpBodies.jsonObject {
		"orgCode" set institute.code
		"name" set encrypt(name)
		"birthday" set encrypt(birthday)
		"stdntPNo" set pageNumber
		"loginType" set loginType.name
	}
).toJsonLoose(UsersIdentifier.serializer())
