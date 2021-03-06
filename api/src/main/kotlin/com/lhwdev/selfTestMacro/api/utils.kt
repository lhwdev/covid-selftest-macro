package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


object YesNoSerializer : KSerializer<Boolean> {
	override val descriptor =
		PrimitiveSerialDescriptor("com.lhwdev.selfTestMacro.api.yesno", PrimitiveKind.STRING)
	
	override fun deserialize(decoder: Decoder) = decoder.decodeString() == "Y"
	override fun serialize(encoder: Encoder, value: Boolean) {
		encoder.encodeString(if(value) "Y" else "N")
	}
}

object IntAsStringSerializer : KSerializer<Int> {
	override val descriptor =
		PrimitiveSerialDescriptor(IntAsStringSerializer::class.java.name, PrimitiveKind.STRING)
	
	override fun deserialize(decoder: Decoder) = decoder.decodeString().toInt()
	
	override fun serialize(encoder: Encoder, value: Int) {
		encoder.encodeString(value.toString())
	}
}
