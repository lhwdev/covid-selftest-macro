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


/*
 * usersId token --(validatePassword)--> users token(temporary) --(selectUserGroup)--> user token
 */

sealed class PasswordResult {
	abstract val isSuccess: Boolean
}

@Serializable(UsersToken.Serializer::class)
data class UsersToken(val token: String) : PasswordResult() {
	override val isSuccess get() = true
	
	object Serializer : KSerializer<UsersToken> {
		override val descriptor =
			PrimitiveSerialDescriptor(UsersToken::class.java.name, PrimitiveKind.STRING)
		
		override fun deserialize(decoder: Decoder) = UsersToken(decoder.decodeString())
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
data class PasswordWrong(
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
					현재 ${data.failedCount}회 틀리셨습니다"
					""".trimIndent()
			1003 -> "비밀번호가 초기화되었습니다. 다시 로그인하세요."
			else -> null
		}
	
	override val isSuccess get() = false
	
	@Serializable
	data class Data(
		@SerialName("failCnt") val failedCount: Int
	)
}

suspend fun validatePassword(
	institute: InstituteInfo,
	userIdentifier: UserIdentifier,
	password: String
): PasswordResult = ioTask {
	val body = fetch(
		institute.requestUrl2["validatePassword"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf(
			"Content-Type" to ContentTypes.json,
			"Authorization" to userIdentifier.token.token
		),
		body = """{"password": "${encrypt(password)}", "deviceUuid": ""}"""
	).toResponseString()
	try {
		Json { ignoreUnknownKeys = true }.decodeFromString(PasswordWrong.serializer(), body)
	} catch(e: Throwable) {
		val userToken = body.removeSurrounding("\"")
		require(userToken.startsWith("Bearer")) { userToken }
		UsersToken(userToken)
	}
}
