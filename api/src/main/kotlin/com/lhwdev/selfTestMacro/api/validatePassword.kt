package com.lhwdev.selfTestMacro.api

import com.lhwdev.fetch.*
import com.lhwdev.fetch.http.HttpMethod
import com.lhwdev.fetch.http.Session
import com.lhwdev.fetch.http.fetch
import com.lhwdev.selfTestMacro.sDefaultFakeHeader
import com.lhwdev.selfTestMacro.transkey.Transkey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


val transkeyUrl: URL = URL("https://hcs.eduro.go.kr/transkeyServlet")


/*
 * usersId token --(validatePassword)--> users token(temporary) --(selectUserGroup)--> user token
 */
sealed class PasswordResult {
	abstract val isSuccess: Boolean
	
	@Serializable
	data class Success(
		@Serializable(YesNoSerializer::class)
		@SerialName("pInfAgrmYn")
		val agreement: Boolean,
		
		val token: UsersToken
	) : PasswordResult() {
		override val isSuccess: Boolean get() = true
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
	data class Failed(
		val isError: Boolean,
		val statusCode: Int,
		val errorCode: Int,
		val data: Data
	) : PasswordResult() {
		override val isSuccess get() = false
		
		val errorMessage: String? = when(statusCode) {
			252 -> when(errorCode) {
				1000 -> "비밀번호를 5회 틀려서 5분후 재시도 하실 수 있어요."
				1001 -> """
				사용자 비밀번호가 맞지 않아요.
				본인이나 가족이 이미 설정한 비밀번호를 입력해 주세요.
				5회 틀리실 경우 5분후에 재시도할 수 있어요.
				현재 ${data.failedCount}회 틀리셨습니다.
			""".trimIndent()
				1003 -> "비밀번호가 초기화되었으니 다시 로그인해주세요."
				else -> null
			}
			255 -> when(errorCode) {
				1004 -> "입력시간이 초과되어 다시 비밀번호를 입력해주세요."
				else -> null
			}
			else -> null
		}
		
		override fun toString(): String = when(errorCode) {
			1000 -> "비밀본호를 5회 틀리셔서 5분 후 재시도하실 수 있습니다."
			1001 -> "비밀번호가 맞지 않습니다. 현재 ${data.failedCount}회 실패하셨습니다."
			1003 -> "비밀번호가 초기화되었습니다. 다시 로그인하세요."
			else -> "알 수 없는 오류: 에러코드 $errorCode (틀린 횟수: ${data.failedCount})"
		}
		
		@Serializable
		data class Data(
			@SerialName("failCnt") val failedCount: Int
		)
	}
	
}

@Serializable(UsersToken.Serializer::class)
data class UsersToken(val token: String) {
	object Serializer : KSerializer<UsersToken> {
		override val descriptor = PrimitiveSerialDescriptor(UsersToken::class.java.name, PrimitiveKind.STRING)
		override fun deserialize(decoder: Decoder) = UsersToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: UsersToken) {
			encoder.encodeString(value.token)
		}
	}
}


private val json = Json { ignoreUnknownKeys = true }


suspend fun Session.validatePassword(
	institute: InstituteInfo,
	userIdentifier: UserIdentifier,
	password: String
): PasswordResult {
	val transkey = Transkey(this, transkeyUrl, Random)
	
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
		institute.requestUrl["validatePassword"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf(
			"Authorization" to userIdentifier.token.token,
			"Accept" to "application/json, text/plain, */*"
		),
		body = Bodies.jsonObject {
			"password" set raonPassword
			"deviceUuid" set ""
			"makeSession" set true
		}
	).getText()
	
	return try {
		json.decodeFromString(PasswordResult.Success.serializer(), result)
	} catch(e: Throwable) {
		json.decodeFromString(PasswordResult.Failed.serializer(), result)
	}
}
