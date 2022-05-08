package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpMethod


// you must inform user when using this api: https://hcs.eduro.go.kr/agreement
@DangerousHcsApi
public suspend fun HcsSession.updateAgreement(token: UsersIdToken) {
	fetch(
		requestUrl["/v2/updatePInfAgrmYn"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
		body = Bodies.jsonObject {}
	)
}


public suspend fun HcsSession.hasPassword(token: UsersIdToken): Boolean = fetch(
	requestUrl["/v2/hasPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token)
).getText().toBooleanStrict()

public suspend fun HcsSession.registerPassword(
	token: UsersIdToken,
	password: String,
	deviceUuid: String = "",
	upperUserToken: UsersToken? = null
): Boolean = fetch(
	requestUrl["/v2/registerPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
	body = Bodies.jsonObject {
		"password" set encrypt(password)
		"deviceUuid" set deviceUuid
		if(upperUserToken != null) "upperToken" set upperUserToken.token
	}
).getText().toBooleanStrict() // TODO: confirm this


public enum class ChangePasswordResult { success, lastNotMatched, wrongNewPassword }

@DangerousHcsApi
public suspend fun HcsSession.changePassword(
	token: UsersToken,
	lastPassword: String,
	newPassword: String
): ChangePasswordResult {
	if(newPassword.isBlank()) return ChangePasswordResult.wrongNewPassword
	
	val result = fetch(
		requestUrl["/v2/changePassword"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
		body = Bodies.jsonObject {
			"password" set encrypt(lastPassword)
			"newPassword" set encrypt(newPassword)
		}
	)
	
	return if(result.isOk) {
		ChangePasswordResult.success
	} else {
		ChangePasswordResult.lastNotMatched
	}
}
