package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


val JsonEncodeDefaults = Json { encodeDefaults = true }
val JsonLoose = Json { ignoreUnknownKeys = true }


object YesNoSerializer : KSerializer<Boolean> {
	override val descriptor = PrimitiveSerialDescriptor("com.lhwdev.selfTestMacro.api.yesno", PrimitiveKind.STRING)
	override fun deserialize(decoder: Decoder) = decoder.decodeString() == "Y"
	override fun serialize(encoder: Encoder, value: Boolean) {
		encoder.encodeString(if(value) "Y" else "N")
	}
}
