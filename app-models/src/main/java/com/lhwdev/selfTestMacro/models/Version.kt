package com.lhwdev.selfTestMacro.models

import com.lhwdev.selfTestMacro.splitTwo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


// 'major.minor(.patch)(-preRelease)'
// ex: 3.0.0-alpha01
@Serializable(with = Version.VersionSerializer::class)
data class Version(
	val major: Int,
	val minor: Int,
	val patch: Int?,
	val preRelease: PreRelease?
) : Comparable<Version> {
	constructor(major: Int, minor: Int, patch: Int?, preRelease: PreRelease?, stringCache: String?)
		: this(major, minor, patch, preRelease) {
		this.stringCache = stringCache
	}
	
	@Transient
	private var stringCache: String? = null
	
	object VersionSerializer : KSerializer<Version> {
		override val descriptor: SerialDescriptor =
			PrimitiveSerialDescriptor(Version::class.qualifiedName!!, PrimitiveKind.STRING)
		
		override fun deserialize(decoder: Decoder): Version = Version(decoder.decodeString())
		
		override fun serialize(encoder: Encoder, value: Version) {
			encoder.encodeString(value.toString())
		}
	}
	
	override fun compareTo(other: Version) = when {
		major != other.major -> major - other.major
		minor != other.minor -> minor - other.minor
		patch != other.patch -> (patch ?: 0) - (other.patch ?: 0)
		preRelease != other.preRelease -> when { // one containing preRelease is considered earier
			preRelease == null -> 1 // other -> this
			other.preRelease == null -> -1 // this -> other
			else -> preRelease.compareTo(other.preRelease) // match by string
		}
		else -> 0
	}
	
	override fun toString(): String = stringCache ?: run {
		val string = buildString {
			append(major)
			append('.')
			append(minor)
			append('.')
			append(patch)
			if(preRelease != null) append(preRelease)
		}
		stringCache = string
		string
	}
}


@Serializable
data class PreRelease(val name: Kind, val version: Int) : Comparable<PreRelease> {
	enum class Kind { // See https://en.wikipedia.org/wiki/Software_release_life_cycle.
		// pre-alpha
		preview, // Preview: from Compose inspection mode
		build, // Build: just nothing
		dev, // Dev: just nothing with different name
		test, // Test: just nothing but for test
		
		// alpha
		alpha, // Alpha: to be tested by developers; not well tested
		
		// beta
		beta, // Beta: some public testing & feedback stage
		snapshot, // Snapshot: prone to change
		m, // Milestone: some feature sets
		rc // Release Candidate: beta version with potential to stable
		// nothing(preRelease = null) means it's stable release
	}
	
	override fun compareTo(other: PreRelease): Int {
		return when {
			name != other.name -> name.ordinal - other.name.ordinal
			else -> version - other.version
		}
	}
	
	override fun toString(): String = "$name$version"
}


fun Version(string: String): Version {
	val (version, preRelease) = if('-' in string) {
		string.splitTwo('-')
	} else {
		string to null
	}
	
	val split = version.split('.').map { it.toInt() }
	return Version(
		major = split[0],
		minor = split[1],
		patch = split.getOrNull(2),
		preRelease = preRelease?.let { PreRelease(it) },
		stringCache = string
	)
}

fun PreRelease(string: String): PreRelease {
	val index = string.indexOfFirst { it.isDigit() }
	if(index == -1) return PreRelease(PreRelease.Kind.valueOf(string), -1)
	return PreRelease(PreRelease.Kind.valueOf(string.take(index)), string.drop(index).toInt())
}

@Serializable(with = VersionSpecSerializer::class)
data class VersionSpec(val from: Version, val to: Version) {
	operator fun contains(version: Version) = version in from..to
	
	override fun toString() = "$from..$to"
}

object VersionSpecSerializer : KSerializer<VersionSpec> {
	override val descriptor = PrimitiveSerialDescriptor(VersionSpec::class.qualifiedName!!, PrimitiveKind.STRING)
	
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
