@file:Suppress("SpellCheckingInspection", "INVISIBLE_REFERENCE")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


public enum class LoginType { school, univ, office }

@Serializable
public data class GetUserTokenRequestBody internal constructor(
	@SerialName("orgCode") val instituteCode: String,
	@SerialName("name") val encryptedName: String,
	@SerialName("birthday") val encryptedBirthday: String,
	@SerialName("stdntPNo") val pageNumber: Int? = null,
	@SerialName("loginType") val loginType: LoginType,
)

@Serializable(UsersIdToken.Serializer::class)
public data class UsersIdToken(val token: String) {
	public object Serializer : KSerializer<UsersIdToken> {
		override val descriptor: SerialDescriptor =
			PrimitiveSerialDescriptor(UsersIdToken::class.java.name, PrimitiveKind.STRING)
		
		override fun deserialize(decoder: Decoder): UsersIdToken = UsersIdToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: UsersIdToken) {
			encoder.encodeString(value.token)
		}
	}
}

internal suspend fun GetUserTokenRequestBody(
	institute: InstituteInfo,
	name: String,
	birthday: String,
	loginType: LoginType
) = GetUserTokenRequestBody(
	instituteCode = institute.code,
	encryptedName = encrypt(name),
	encryptedBirthday = encrypt(birthday),
	loginType = loginType
)

/*
 * admnYn: "N"
 * atptOfcdcConctUrl: "??????.eduro.go.kr"
 * mngrClassYn: "N"
 * mngrDeptYn: "N"
 * orgName: "??????"
 * pInfAgrmYn: "Y"
 * stdntYn: "Y"
 * token: "Bearer ??.???.??"
 * userName: "???"
 */

@Serializable
public data class UsersIdentifier(
	@SerialName("userName") val mainUserName: String,
	@SerialName("token") val token: UsersIdToken,
	@Serializable(with = YesNoSerializer::class) @SerialName("stdntYn") val isStudent: Boolean,
	@Serializable(with = YesNoSerializer::class) @SerialName("pInfAgrmYn") val agreement: Boolean
)


// TODO: multiple users with same institute, name, birthday, loginType (that exists in hcs code)
public suspend fun Session.findUser(
	institute: InstituteInfo,
	name: String,
	birthday: String,
	loginType: LoginType
): UsersIdentifier = fetch(
	institute.requestUrl2["findUser"],
	method = HttpMethod.post,
	headers = sDefaultFakeHeader,
	body = HttpBodies.json(
		GetUserTokenRequestBody.serializer(),
		GetUserTokenRequestBody(
			institute = institute,
			name = name,
			birthday = birthday,
			loginType = loginType
		),
		json = JsonEncodeDefaults
	)
).toJsonLoose(UsersIdentifier.serializer())
