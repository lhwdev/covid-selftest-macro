package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*


// you must inform user when using this api: https://hcs.eduro.go.kr/agreement
public suspend fun Session.updateAgreement(institute: InstituteInfo, usersIdentifier: UsersIdentifier) {
	fetch(
		institute.requestUrl2["updatePInfAgrmYn"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Authorization" to usersIdentifier.token.token),
		body = HttpBodies.jsonObject {}
	)
}


public suspend fun Session.hasPassword(
	institute: InstituteInfo,
	usersIdentifier: UsersIdentifier
): Boolean = fetch(
	institute.requestUrl2["hasPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to usersIdentifier.token.token)
).getText().toBooleanStrict()

public suspend fun Session.registerPassword(
	institute: InstituteInfo,
	usersIdentifier: UsersIdentifier,
	password: String,
	deviceUuid: String = "",
	upperUserToken: UsersToken? = null
): Boolean = fetch(
	institute.requestUrl2["registerPassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to usersIdentifier.token.token),
	body = HttpBodies.jsonObject {
		"password" set encrypt(password)
		"deviceUuid" set deviceUuid
		if(upperUserToken != null) "upperToken" set upperUserToken.token
	}
).getText().toBooleanStrict() // TODO: confirm this

public suspend fun Session.changePassword(
	institute: InstituteInfo,
	usersIdentifier: UsersIdentifier,
	lastPassword: String,
	newPassword: String
): Boolean = fetch(
	institute.requestUrl2["changePassword"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader + mapOf("Authorization" to usersIdentifier.token.token),
	body = HttpBodies.jsonObject {
		"password" set encrypt(lastPassword)
		"newPassword" set encrypt(newPassword)
	}
).getText().toBooleanStrict()
