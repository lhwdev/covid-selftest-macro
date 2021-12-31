package com.lhwdev.selfTestMacro.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer


/**
 * Warning: do not trust things inside this class if coming from external sources.
 * Or, DO NOT MAKE THIS EXTERNAL.
 *
 * Poor implementation; low performance.
 */
@Serializable(DynamicSerializable.Serializer::class)
class DynamicSerializable<T : Any>(
	val className: String,
	val value: T
) {
	@Serializable
	private class Data(val className: String, val value: String)
	
	class Serializer<T : Any>(val typeParameter: T) : KSerializer<DynamicSerializable<T>> {
		override val descriptor: SerialDescriptor
			get() = Data.serializer().descriptor
		
		@OptIn(ExperimentalSerializationApi::class)
		override fun deserialize(decoder: Decoder): DynamicSerializable<T> {
			val data = decoder.decodeSerializableValue(Data.serializer())
			@Suppress("UNCHECKED_CAST")
			return DynamicSerializable(
				data.className,
				Json.decodeFromString(serializer(Class.forName(data.className)) as KSerializer<T>, data.value)
			)
		}
		
		override fun serialize(encoder: Encoder, value: DynamicSerializable<T>) {
			encoder.encodeSerializableValue(
				Data.serializer(),
				Data(value.className, Json.encodeToString(serializer(Class.forName(value.className)), value.value))
			)
		}
	}
}


inline fun <reified T : Any> DynamicSerializable(value: T): DynamicSerializable<T> = DynamicSerializable(
	className = T::class.java.name,
	value = value
)
