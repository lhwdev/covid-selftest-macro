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


sealed class PasswordResult {
	abstract val isSuccess: Boolean
}

@Serializable
data class User(val id: UserId, val token: UserToken)

@Serializable(UserToken.Serializer::class)
data class UserToken(val token: String) : PasswordResult() {
	override val isSuccess get() = true
	object Serializer : KSerializer<UserToken> {
		override val descriptor = PrimitiveSerialDescriptor(UserToken::class.java.name, PrimitiveKind.STRING)
		override fun deserialize(decoder: Decoder) = UserToken(decoder.decodeString())
		override fun serialize(encoder: Encoder, value: UserToken) {
			encoder.encodeString(value.token)
		}
	}
}

// {isError: true, statusCode: 252, errorCode: 1001, data: {failCnt: 1, canInitPassword: false}}
@Serializable
data class PasswordWrong(
	val isError: Boolean,
	val statusCode: Int,
	val errorCode: Int,
	val data: Data
) : PasswordResult() {
	override val isSuccess get() = false
	@Serializable
	data class Data(
		@SerialName("failCnt") val failedCount: Int
	)
}

suspend fun validatePassword(institute: InstituteInfo, token: UserIdToken, password: String): PasswordResult = ioTask {
	val body = fetch(
		institute.requestUrl["validatePassword"],
		method = HttpMethod.post,
		headers = sDefaultFakeHeader + mapOf("Content-Type" to ContentTypes.json, "Authorization" to token.token),
		body = """{"password": "${encrypt(password)}", "deviceUuid": ""}"""
	).toResponseString()
	try {
		Json { ignoreUnknownKeys = true }.decodeFromString(PasswordWrong.serializer(), body)
	} catch(e: Throwable) {
		require(body.startsWith("Bearer"))
		UserToken(body)
	}
}
