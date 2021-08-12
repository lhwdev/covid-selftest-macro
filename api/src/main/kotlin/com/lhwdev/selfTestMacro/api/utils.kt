package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


@RequiresOptIn(message = "This api is dangerous to use. You should get user information in 'right' and confirm user before calling this api.")
public annotation class DangerousHcsApi


internal val JsonEncodeDefaults = Json { encodeDefaults = true }
internal val JsonLoose = Json { ignoreUnknownKeys = true }


public object YesNoSerializer : KSerializer<Boolean> {
	override val descriptor: SerialDescriptor =
		PrimitiveSerialDescriptor("com.lhwdev.selfTestMacro.api.yesno", PrimitiveKind.STRING)
	
	override fun deserialize(decoder: Decoder): Boolean = decoder.decodeString() == "Y"
	override fun serialize(encoder: Encoder, value: Boolean) {
		encoder.encodeString(if(value) "Y" else "N")
	}
}

public object IntAsStringSerializer : KSerializer<Int> {
	override val descriptor: SerialDescriptor =
		PrimitiveSerialDescriptor(IntAsStringSerializer::class.java.name, PrimitiveKind.STRING)
	
	override fun deserialize(decoder: Decoder): Int = decoder.decodeString().toInt()
	
	override fun serialize(encoder: Encoder, value: Int) {
		encoder.encodeString(value.toString())
	}
}
