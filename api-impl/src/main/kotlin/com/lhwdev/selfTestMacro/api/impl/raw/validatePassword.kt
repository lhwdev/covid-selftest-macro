package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.selfTestMacro.api.InternalHcsApi


@Suppress("SpellCheckingInspection")
@InternalHcsApi
public suspend fun HcsSession.validatePassword(
	previousToken: UsersToken,
	password: String,
	deviceUuid: String = ""
): PasswordResult {
	val raonPassword = raonPassword(password)
	
	val result = fetch(
		requestUrl["/v2/validatePassword"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Accept" to "application/json, text/plain, */*"),
		body = Bodies.jsonObject {
			"password" set raonPassword
			"deviceUuid" set deviceUuid
			"makeSession" set true
		}
	).getText()
	
	return try {
		JsonLoose.decodeFromString(PasswordResult.Success.serializer(), result)
	} catch(e: Throwable) {
		JsonLoose.decodeFromString(PasswordResult.Failed.serializer(), result)
	}
}

