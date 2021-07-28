@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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

