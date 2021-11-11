package com.lhwdev.fetch.headers

import com.lhwdev.fetch.FetchHeader
import com.lhwdev.fetch.FetchHeaderKey
import com.lhwdev.fetch.FetchHeaders
import com.lhwdev.fetch.MutableFetchHeaders
import com.lhwdev.selfTestMacro.splitTwo
import java.nio.charset.Charset


object ContentTypes {
	const val json = "application/json;charset=utf-8"
}


val FetchHeaders.contentType: ContentType?
	get() = this[ContentType.Key]

var MutableFetchHeaders.contentType: ContentType?
	get() = this[ContentType.Key]
	set(value) {
		this[ContentType.Key] = value
	}


class ContentType(val mediaType: String, val charsetName: String? = null, val boundary: String? = null) : FetchHeader {
	companion object {
		val Key = FetchHeaderKey("Content-Type") { contentTypeOf(it) }
	}
	
	val charset: Charset get() = charsetName?.let { charset(it) } ?: Charsets.UTF_8
	
	override fun serialize(): String = buildString {
		append(mediaType)
		if(charsetName != null) {
			append(";charset=")
			append(charsetName)
		}
		if(boundary != null) {
			append(";boundary=")
			append(boundary)
		}
	}
}


// https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.1.1
fun contentTypeOf(from: String): ContentType {
	val list = from.split(';')
	val mediaType = list[0].lowercase()
	if(list.size == 1) return ContentType(mediaType)
	
	val params = list.drop(1).associate {
		val (key, value) = it.splitTwo('=')
		key.lowercase() to value.removeSurrounding("\"").lowercase()
	}
	
	return ContentType(
		mediaType = mediaType,
		charsetName = params["charset"],
		boundary = params["boundary"]
	)
}
