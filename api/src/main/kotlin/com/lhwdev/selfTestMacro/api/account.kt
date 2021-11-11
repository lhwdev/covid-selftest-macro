package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch


// you must inform user when using this api: https://hcs.eduro.go.kr/agreement
@DangerousHcsApi
public suspend fun Session.updateAgreement(institute: InstituteInfo, token: UsersIdToken) {
	fetch(
		institute.requestUrl2["updatePInfAgrmYn"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
		body = Bodies.jsonObject {}
	)
}


public suspend fun Session.hasPassword(
	institute: InstituteInfo,
	token: UsersIdToken
): Boolean = fetch(
	institute.requestUrl2["hasPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token)
).getText().toBooleanStrict()

public suspend fun Session.registerPassword(
	institute: InstituteInfo,
	token: UsersIdToken,
	password: String,
	deviceUuid: String = "",
	upperUserToken: UsersToken? = null
): Boolean = fetch(
	institute.requestUrl2["registerPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to token.token),
	body = Bodies.jsonObject {
		"password" set encrypt(password)
		"deviceUuid" set deviceUuid
		"upperToken" set upperUserToken?.token
	}
).getText().toBooleanStrict() // TODO: confirm this


public enum class ChangePasswordResult { success, lastNotMatched, wrongNewPassword }

@DangerousHcsApi
public suspend fun Session.changePassword(
	institute: InstituteInfo,
	token: UsersToken,
	lastPassword: String,
	newPassword: String
): ChangePasswordResult {
	if(newPassword.isBlank()) return ChangePasswordResult.wrongNewPassword
	
	val result = fetch(
		institute.requestUrl2["changePassword"],
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
