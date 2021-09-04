package com.lhwdev.fetch

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URL
import java.net.URLEncoder

val sDefaultFakeHeader = mapOf(
	"User-Agent" to "Mozilla/5.0 (Linux; Android 7.0; SM-G892A Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/85.0.4183.81 Mobile Safari/537.36",
	"X-Requested-With" to "XMLHttpRequest"
)

operator fun URL.get(childPath: String): URL = URL(
	this,
	buildString {
		append(path.removeSuffix("/"))
		append('/')
		append(childPath)
	}
)

operator fun URL.get(vararg params: Pair<String, String>): URL = URL(
	this,
	buildString {
		append(path.removeSuffix("/"))
		append('?')
		append(queryUrlParamsToString(*params))
	}
)

operator fun URL.get(childPath: String, vararg params: Pair<String, String>): URL = URL(
	this,
	buildString {
		append(path.removeSuffix("/"))
		append('/')
		append(childPath)
		append('?')
		append(queryUrlParamsToString(*params))
	}
)

object URLSerializer : KSerializer<URL> {
	override val descriptor =
		PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)
	
	override fun serialize(encoder: Encoder, value: URL) = encoder.encodeString(value.toString())
	override fun deserialize(decoder: Decoder) = URL(decoder.decodeString())
}

// a=urlencoded1&b=2&c=3...
// this does not encode keys
fun queryUrlParamsToString(vararg params: Pair<String, String>): String =
	queryUrlParamsToString(mapOf(*params))

fun queryUrlParamsToString(params: Map<String, String>): String =
	params.entries.joinToString(separator = "&") { (k, v) ->
		"$k=${URLEncoder.encode(v, "UTF-8")}"
	}
