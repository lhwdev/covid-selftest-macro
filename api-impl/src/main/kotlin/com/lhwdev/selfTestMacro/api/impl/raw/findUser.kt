@file:Suppress("SpellCheckingInspection", "INVISIBLE_REFERENCE")

package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.Bodies
import com.lhwdev.fetch.get
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.jsonObject
import com.lhwdev.fetch.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.toJsonLoose


// TODO: multiple users with same institute, name, birthday, loginType (that exists in hcs code)
public suspend fun HcsSession.findUser(
	instituteCode: String,
	name: String,
	birthday: String,
	loginType: LoginType,
	searchKey: SearchKey,
	pageNumber: Int? = null
): UsersIdentifier = fetch(
	requestUrl["/v2/findUser"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader,
	body = Bodies.jsonObject {
		"orgCode" set instituteCode
		"name" set encrypt(name)
		"birthday" set encrypt(birthday)
		if(pageNumber != null) "stdntPNo" set pageNumber
		"searchKey" set searchKey.token
		"loginType" set loginType.name
	}
).toJsonLoose(UsersIdentifier.serializer())
