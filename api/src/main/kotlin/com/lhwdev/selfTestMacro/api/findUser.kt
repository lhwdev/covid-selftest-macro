@file:Suppress("SpellCheckingInspection", "INVISIBLE_REFERENCE")

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher


enum class LoginType { school, univ, office }

@Serializable
data class GetUserTokenRequestBody internal constructor(
	@SerialName("orgCode") val instituteCode: String,
	@SerialName("name") val encryptedName: String,
	@SerialName("birthday") val encryptedBirthday: String,
	@SerialName("stdntPNo") val pageNumber: Int? = null,
	@SerialName("loginType") val loginType: LoginType,
)

@Serializable(UsersIdToken.Serializer::class)
data class UsersIdToken(val token: String) {
	object Serializer : KSerializer<UsersIdToken> {
		override val descriptor =
			PrimitiveSerialDescriptor(UsersIdToken::class.java.name, PrimitiveKind.STRING)
		
		override fun deserialize(decoder: Decoder) = UsersIdToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: UsersIdToken) {
			encoder.encodeString(value.token)
		}
	}
}

suspend fun GetUserTokenRequestBody(
	institute: InstituteInfo,
	name: String,
	birthday: String,
	loginType: LoginType
) =
	GetUserTokenRequestBody(institute.code, encrypt(name), encrypt(birthday), loginType = loginType)

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
data class UserIdentifier(
	@SerialName("userName") val mainUserName: String,
	@SerialName("token") val token: UsersIdToken,
	@Serializable(with = YesNoSerializer::class) @SerialName("stdntYn") val isStudent: Boolean
)



suspend fun findUser(institute: InstituteInfo, request: GetUserTokenRequestBody) = ioTask {
	fetch(
		institute.requestUrl2["findUser"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json),
		body = Json { encodeDefaults = true }.encodeToString(
			GetUserTokenRequestBody.serializer(),
			request
		)
	).toJsonLoose(UserIdentifier.serializer())
}


lateinit var encodeBase64: (ByteArray) -> String


suspend fun encrypt(string: String): String = ioTask {
	val key = RSAPublicKeySpec(
		BigInteger("30718937712611605689191751047964347711554702318809238360089112453166217803395521606458190795722565177328746277011809492198037993902927400109154434682159584719442248913593972742086295960255192532052628569970645316811605886842040898815578676961759671712587342568414746446165948582657737331468733813122567503321355924190641302039446055143553127897636698729043365414410208454947672037202818029336707554263659582522814775377559532575089915217472518288660143323212695978110773753720635850393399040827859210693969622113812618481428838504301698541638186158736040620420633114291426890790215359085924554848097772407366395041461"),
		BigInteger("65537")
	)
	val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
	cipher.init(Cipher.PUBLIC_KEY, KeyFactory.getInstance("RSA").generatePublic(key))
	encodeBase64(cipher.doFinal(string.toByteArray()))
}
