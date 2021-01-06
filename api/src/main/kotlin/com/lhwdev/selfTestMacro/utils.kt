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


operator fun URL.get(childPath: String) = URL(this, path.removeSuffix("/") + "/" + childPath)

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
		PrimitiveSerialDescriptor(URL::class.java.name, PrimitiveKind.STRING)
	
	override fun serialize(encoder: Encoder, value: URL) = encoder.encodeString(value.toString())
	override fun deserialize(decoder: Decoder) = URL(decoder.decodeString())
}


// a=urlencoded1&b=2&c=3...
// this does not encode keys
fun queryUrlParamsToString(params: Map<String, String>) =
	params.entries.joinToString(separator = "&") { (k, v) ->
		"$k=${URLEncoder.encode(v, "UTF-8")}"
	}

