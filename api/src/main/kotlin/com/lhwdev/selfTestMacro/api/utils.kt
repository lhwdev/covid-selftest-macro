@file:OptIn(ExperimentalSerializationApi::class)

package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


@RequiresOptIn(message = "This api is dangerous to use. You should get user information in 'right' way and confirm user before calling this api.")
public annotation class DangerousHcsApi


public val JsonEncodeDefaults: Json = Json { encodeDefaults = true }
public val JsonLoose: Json = Json { ignoreUnknownKeys = true }


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


public open class PrimitiveMappingSerializer<Data, Raw>(
	private val rawSerializer: KSerializer<Raw>,
	serialName: String,
	primitiveKind: PrimitiveKind,
	private vararg val map: Pair<Data, Raw>
) : KSerializer<Data> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName = serialName, kind = primitiveKind)
	
	override fun deserialize(decoder: Decoder): Data {
		val raw = rawSerializer.deserialize(decoder)
		return (map.find { it.second == raw } ?: error("Could not find $raw (from ${descriptor.serialName})")).first
	}
	
	override fun serialize(encoder: Encoder, value: Data) {
		val raw =
			(map.find { it.first == value } ?: error("Could not find $value (from ${descriptor.serialName})")).second
		rawSerializer.serialize(encoder, raw)
	}
}
