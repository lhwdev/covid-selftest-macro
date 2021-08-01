@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.net.HttpCookie
import java.net.URL
import java.net.URLEncoder


val sDefaultFakeHeader = mapOf(
	"User-Agent" to "Mozilla/5.0 (Linux; Android 7.0; SM-G892A Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/85.0.4183.81 Mobile Safari/537.36",
	"X-Requested-With" to "XMLHttpRequest"
)


operator fun URL.get(childPath: String? = null) = URL(
	this,
	buildString {
		append(path.removeSuffix("/"))
		if(childPath != null) append("/$childPath")
	}
)

operator fun URL.get(vararg params: Pair<String, String>) = URL(
	this,
	buildString {
		append(path.removeSuffix("/"))
		append('?')
		append(queryUrlParamsToString(mapOf(*params)))
	}
)

// our project is too small to use form like 'major.minor.bugFixes'
@Serializable
data class Version(val major: Int, val minor: Int) : Comparable<Version> {
	override fun compareTo(other: Version) =
		if(major == other.major) minor - other.minor
		else major - other.major
	
	override fun toString() = "$major.$minor"
}


fun Version(string: String): Version {
	val split = string.split('.').map { it.toInt() }
	require(split.size == 2)
	return Version(split[0], split[1])
}

@Serializable(with = VersionSpecSerializer::class)
data class VersionSpec(val from: Version, val to: Version) {
	operator fun contains(version: Version) = version in from..to
	
	override fun toString() = "$from..$to"
}

object VersionSpecSerializer : KSerializer<VersionSpec> {
	override val descriptor = PrimitiveSerialDescriptor(VersionSpec::class.java.name, PrimitiveKind.STRING)
	
	override fun deserialize(decoder: Decoder) = VersionSpec(decoder.decodeString())
	
	override fun serialize(encoder: Encoder, value: VersionSpec) {
		encoder.encodeString(value.toString())
	}
}


fun VersionSpec(string: String): VersionSpec {
	val index = string.indexOf("..")
	if(index == -1) return Version(string).let { VersionSpec(it, it) }
	val from = string.take(index)
	val to = string.drop(index + 2)
	return VersionSpec(Version(from), Version(to))
}


object URLSerializer : KSerializer<URL> {
	override val descriptor =
		PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)
	
	override fun serialize(encoder: Encoder, value: URL) = encoder.encodeString(value.toString())
	override fun deserialize(decoder: Decoder) = URL(decoder.decodeString())
}


// a=urlencoded1&b=2&c=3...
// this does not encode keys
fun queryUrlParamsToString(params: Map<String, String>) =
	params.entries.joinToString(separator = "&") { (k, v) ->
		"$k=${URLEncoder.encode(v, "UTF-8")}"
	}


object HttpCookieSerializer : KSerializer<HttpCookie> {
	@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
	override val descriptor: SerialDescriptor = buildClassSerialDescriptor("java.net.HttpCookie") {
		element("name", String.serializer().descriptor)
		element("value", String.serializer().descriptor)
		element("comment", String.serializer().descriptor, isOptional = true)
		element("commentURL", String.serializer().descriptor, isOptional = true)
		element("domain", String.serializer().descriptor)
		element("maxAge", Long.serializer().descriptor)
		element("path", String.serializer().descriptor)
		element("portlist", String.serializer().descriptor, isOptional = true)
		element("version", Int.serializer().descriptor)
		element("secure", Boolean.serializer().descriptor)
		element("discard", Boolean.serializer().descriptor)
	}
	
	override fun serialize(encoder: Encoder, value: HttpCookie): Unit = encoder.encodeStructure(descriptor) {
		val descriptor = descriptor
		
		encodeStringElement(descriptor, 0, value.name)
		encodeStringElement(descriptor, 1, value.value)
		if(value.comment != null) encodeStringElement(descriptor, 2, value.comment)
		if(value.commentURL != null) encodeStringElement(descriptor, 3, value.commentURL)
		encodeStringElement(descriptor, 4, value.domain)
		encodeLongElement(descriptor, 5, value.maxAge)
		encodeStringElement(descriptor, 6, value.path)
		if(value.portlist != null) encodeStringElement(descriptor, 7, value.portlist)
		encodeIntElement(descriptor, 8, value.version)
		encodeBooleanElement(descriptor, 9, value.secure)
		encodeBooleanElement(descriptor, 10, value.discard)
	}
	
	@OptIn(ExperimentalSerializationApi::class)
	override fun deserialize(decoder: Decoder): HttpCookie = decoder.decodeStructure(descriptor) {
		// json do not support decodeSequentially()
		val descriptor = descriptor
		
		var name = ""
		var value = ""
		var comment: String? = null
		var commentURL: String? = null
		var domain = ""
		var maxAge = 0L
		var path = ""
		var portlist: String? = null
		var version = 0
		var secure = false
		var discard = false
		
		while(true) {
			when(decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> name = decodeStringElement(descriptor, 0)
				1 -> value = decodeStringElement(descriptor, 1)
				2 -> comment = decodeStringElement(descriptor, 2)
				3 -> commentURL = decodeStringElement(descriptor, 3)
				4 -> domain = decodeStringElement(descriptor, 4)
				5 -> maxAge = decodeLongElement(descriptor, 5)
				6 -> path = decodeStringElement(descriptor, 6)
				7 -> portlist = decodeStringElement(descriptor, 7)
				8 -> version = decodeIntElement(descriptor, 8)
				9 -> secure = decodeBooleanElement(descriptor, 9)
				10 -> discard = decodeBooleanElement(descriptor, 10)
			}
		}
		
		val cookie = HttpCookie(name, value)
		cookie.comment = comment
		cookie.commentURL = commentURL
		cookie.domain = domain
		cookie.maxAge = maxAge
		cookie.path = path
		cookie.portlist = portlist
		cookie.version = version
		cookie.secure = secure
		cookie.discard = discard
		
		cookie
	}
}

