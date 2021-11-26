@file:Suppress("BlockingMethodInNonBlockingContext")

package com.lhwdev.fetch

import com.lhwdev.fetch.http.HttpInterceptorImpl
import com.lhwdev.fetch.http.HttpIoException
import com.lhwdev.fetch.http.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder


fun interface FetchInterceptor {
	suspend fun intercept(
		url: URL,
		method: FetchMethod?,
		headers: Map<String, String>,
		session: Session?,
		body: FetchBody?
	): FetchResult?
}


val sFetchInterceptors = mutableListOf<FetchInterceptor>(HttpInterceptorImpl)


interface FetchBody {
	fun write(out: OutputStream)
	
	fun writeDebug(out: OutputStream): Unit = error("debug not capable")
	val debugAvailable: Boolean get() = false
	val contentType: String? get() = null
}


interface FetchMethod

interface FetchResult {
	val responseCode: Int
	val responseCodeMessage: String
	val rawResponse: InputStream
	
	fun close()
}

fun FetchResult.requireOk() {
	if(responseCode !in 200..299 /* -> ??? */)
		throw HttpIoException(responseCode, responseCodeMessage)
}


suspend inline fun <reified T> FetchResult.toJson(from: Json = Json.Default): T =
	withContext(Dispatchers.IO) { from.decodeFromString(getText()) }

suspend fun <T> FetchResult.toJson(
	serializer: KSerializer<T>,
	from: Json = Json.Default
): T = withContext(Dispatchers.IO) { from.decodeFromString(serializer, getText()) }

suspend fun FetchResult.getText(): String = withContext(Dispatchers.IO) {
	val value = rawResponse.reader().readText()
	rawResponse.close()
	value
}


var sDebugFetch = false


fun readableUrl(url: String): String {
	val index = url.indexOf("?")
	if(index == -1) return url
	val link = url.substring(0, index)
	val attrs = url.substring(index + 1)
	return "\u001B[0m$link\u001B[0m?.. (urlParams: " + attrs.split('&').joinToString {
		val eqIndex = it.indexOf('=')
		val k = it.substring(0, eqIndex)
		val v = URLDecoder.decode(it.substring(eqIndex + 1), "UTF-8")
		"\u001B[91m$k \u001B[96m= \u001B[97m'$v'\u001B[0m"
	} + ")"
}


