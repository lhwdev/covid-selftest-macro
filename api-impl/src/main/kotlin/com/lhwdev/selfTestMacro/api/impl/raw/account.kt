package com.lhwdev.selfTestMacro.api.impl.raw

import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.api.UserGroup


// you must inform user when using this api: https://hcs.eduro.go.kr/agreement
@DangerousHcsApi
public suspend fun HcsSession.updateAgreement(token: User.Token) {
	fetch(
		requestUrl["/v2/updatePInfAgrmYn"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
		body = Bodies.jsonObject {}
	)
}


public suspend fun HcsSession.hasPassword(token: User.Token): Boolean = fetch(
	requestUrl["/v2/hasPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token)
).getText().toBooleanStrict()

public suspend fun HcsSession.registerPassword(
	token: User.Token,
	password: String,
	deviceUuid: String = "",
	upperUserToken: UserGroup.Token? = null
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
	token: UserGroup.Token,
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
