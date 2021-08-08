package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.*
import com.lhwdev.selfTestMacro.transkey.Transkey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


public val transkeyUrl: URL = URL("https://hcs.eduro.go.kr/transkeyServlet")


/*
 * usersId token --(validatePassword)--> users token(temporary) --(selectUserGroup)--> user token
 */

public sealed class PasswordResult {
	public abstract val isSuccess: Boolean
}

@Serializable(UsersToken.Serializer::class)
public data class UsersToken(val token: String) : PasswordResult() {
	override val isSuccess: Boolean get() = true
	
	public object Serializer : KSerializer<UsersToken> {
		override val descriptor: SerialDescriptor =
			PrimitiveSerialDescriptor(UsersToken::class.java.name, PrimitiveKind.STRING)
		
		override fun deserialize(decoder: Decoder): UsersToken = UsersToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: UsersToken) {
			encoder.encodeString(value.token)
		}
	}
}

// {isError: true, statusCode: 252, errorCode: 1001, data: {failCnt: 1, canInitPassword: false}}
/* switch(e.data.errorCode):
 * case 1000:
 *     var t = "비밀번호를 5회 틀리셔서 5분후 재시도 하실 수 있습니다.";
 *     void 0 != e.data.data && void 0 != e.data.data.remainMinutes && (t += "\n약 ".concat(e.data.data.remainMinutes, "분 남았습니다")),
 *     5 === e.data.data.failCnt && (t += "\n비밀번호를 잊으셨나요? 학교(기관)로 문의 바랍니다."),
 *     alert(t);
 *     break;
 * case 1001:
 *     if (!1 === e.data.data.canInitPassword) {
 *         var n = "사용자 비밀번호가 맞지 않습니다. \n본인이나 가족이 이미 설정한 비밀번호를 입력하여 주시기 바랍니다.\n5회 틀리실 경우 5분후에 재시도 가능합니다 \n" + "현재 ".concat(e.data.data.failCnt, "회 틀리셨습니다");
 *         alert(n)
 *     }
 *     break;
 * case 1003:
 *     alert("비밀번호가 초기화 되었습니다.\n다시 로그인하세요")
 */
@Serializable
public data class PasswordWrong(
	val isError: Boolean,
	val statusCode: Int,
	val errorCode: Int,
	val data: Data
) : PasswordResult() {
	val errorMessage: String?
		get() = when(errorCode) {
			1000 -> "비밀번호를 5회 틀리셔서 5분후 재시도 하실 수 있습니다."
			1001 -> """
					사용자 비밀번호가 맞지 않습니다.
					본인이나 가족이 이미 설정한 비밀번호를 입력하여 주시기 바랍니다.
					5회 틀리실 경우 5분후에 재시도 가능합니다.
					현재 ${data.failedCount}회 틀리셨습니다.
					""".trimIndent()
			1003 -> "비밀번호가 초기화되었습니다. 다시 로그인하세요."
			else -> null
		}
	
	override val isSuccess: Boolean get() = false
	
	override fun toString(): String = errorMessage ?: "알 수 없는 오류: 에러코드 $errorCode (틀린 횟수: ${data.failedCount})"
	
	@Serializable
	public data class Data(
		@SerialName("failCnt") val failedCount: Int
	)
}

private val json = Json { ignoreUnknownKeys = true }


public suspend fun Session.validatePassword(
	institute: InstituteInfo,
	usersIdentifier: UsersIdentifier,
	password: String
): PasswordResult = withContext(Dispatchers.IO) main@ {
	val transkey = Transkey(this@validatePassword, transkeyUrl, Random)
	
	val keyPad = transkey.newKeypad(
		keyType = "number",
		name = "password",
		inputName = "password",
		fieldType = "password"
	)
	
	val encrypted = keyPad.encryptPassword(password)
	
	val hm = transkey.hmacDigest(encrypted.toByteArray())
	
	val raonPassword = jsonString {
		"raon" jsonArray {
			addJsonObject {
				"id" set "password"
				"enc" set encrypted
				"hmac" set hm
				"keyboardType" set "number"
				"keyIndex" set keyPad.keyIndex
				"fieldType" set "password"
				"seedKey" set transkey.crypto.encryptedKey
				"initTime" set transkey.initTime
				"ExE2E" set "false"
			}
		}
	}
	
	val result = fetch(
		institute.requestUrl2["validatePassword"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf(
			"Authorization" to usersIdentifier.token.token,
			"Accept" to "application/json, text/plain, */*"
		),
		body = HttpBodies.jsonObject {
			"password" set raonPassword
			"deviceUuid" set ""
			"makeSession" set true
		}
	).getText()
	
	fun parseResultToken(): UsersToken {
		val userToken = result.removeSurrounding("\"")
		require(userToken.startsWith("Bearer")) { "Malformed users token $userToken" }
		return UsersToken(userToken)
	}
	
	if(result.startsWith('\"')) {
		try {
			parseResultToken()
		} catch(e: Throwable) {
			json.decodeFromString(PasswordWrong.serializer(), result)
		}
	} else {
		try {
			json.decodeFromString(PasswordWrong.serializer(), result)
		} catch(e: Throwable) {
			parseResultToken()
		}
	}
}
