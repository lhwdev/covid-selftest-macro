package com.lhwdev.selfTestMacro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder


enum class HttpMethod(val requestName: String) {
	get("GET"),
	head("HEAD"),
	post("POST"),
	delete("DELETE"),
	connect("CONNECT"),
	options("OPTIONS"),
	trace("TRACE"),
	patch("PATCH")
}

interface FetchResult {
	val responseCode: Int
	val responseMessage: String
	val response: InputStream
}

private class FetchResultImpl(val connection: HttpURLConnection) :
	FetchResult {
	override val responseCode get() = connection.responseCode
	override val responseMessage: String get() = connection.responseMessage
	override val response: InputStream
		get() {
			requireOk()
			return connection.inputStream
		}
}

class HttpIoException(responseCode: Int, responseMessage: String, cause: Throwable? = null) :
	IOException("HTTP error $responseCode $responseMessage", cause)

fun FetchResult.requireOk() {
	if(responseCode !in 200..299 /* -> ??? */)
		throw HttpIoException(responseCode, responseMessage)
}


fun <T> FetchResult.toJson(serializer: KSerializer<T>, config: Json = Json): T =
	config.decodeFromString(serializer, response.reader().readText())

fun FetchResult.toResponseString() = response.reader().readText()


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

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun DisposeScope.fetch( // for debug
	url: URL,
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	body: (suspend (OutputStream) -> Unit)? = null
): FetchResult = withContext(Dispatchers.IO) {
	if(sDebugFetch) {
		println("")
		
		// open
		val connection = url.openConnection() as HttpURLConnection
		println("\u001b[1;91m<- send HTTP \u001B[93m${HttpMethod.post}\u001b[0m: ${readableUrl(url.toString())}")
		if(body != null) connection.doOutput = true
		
		connection.requestMethod = method.requestName
		
		for((k, v) in headers) {
			connection.setRequestProperty(k, v)
			println("    \u001B[96m$k\u001B[0m: \u001B[97m$v")
		}
		
		// connect
		connection.connect()
		registerDisposal {
			// close
			connection.disconnect()
		}
		
		val response = FetchResultImpl(connection)
		
		println("\u001B[0m")
		
		if(body != null) connection.outputStream.use {
			val out = ByteArrayOutputStream()
			body(out)
			val array = out.toByteArray()
			System.out.write(array)
			it.write(array)
			println()
		} else {
			println("(no body)")
		}
		
		println()
		
		println("\u001B[1;91m-> receive \u001B[93m${connection.headerFields[null]!![0]}\u001B[0m")
		println("  \u001B[35m(message: '${connection.responseMessage}')")
		for((k, v) in connection.headerFields) {
			if(k == null) continue
			println("    \u001B[96m$k\u001B[0m: \u001B[97m${v.joinToString()}")
		}
		println("\u001B[0m")
		
		if(response.responseCode in 200..299) {
			val arr = response.response.readBytes()
			System.out.write(arr)
			println()
			object : FetchResult by response {
				override val response = ByteArrayInputStream(arr)
			}
		} else {
			println("(content: error)")
			response
		}.also {
			println("------------------------")
		}
	} else { // without debug logging
		// open
		val connection = url.openConnection() as HttpURLConnection
		
		if(body != null) connection.doOutput = true
		
		connection.requestMethod = method.requestName
		
		for((k, v) in headers)
			connection.setRequestProperty(k, v)
		
		// connect
		connection.connect()
		
		registerDisposal {
			// close
			connection.disconnect()
		}
		
		val response = FetchResultImpl(connection)
		
		if(body != null) connection.outputStream.use { body(it) }
		
		response
	}
}


suspend fun DisposeScope.fetch(
	url: URL,
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	body: InputStream
) = fetch(url, method, headers) { body.copyTo(it) }

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun DisposeScope.fetch(
	url: URL,
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	body: String
) = fetchWriter(url, method, headers) { it.write(body) }

@Suppress("BlockingMethodInNonBlockingContext")
suspend inline fun DisposeScope.fetchWriter(
	url: URL,
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	crossinline body: suspend (Writer) -> Unit
) = fetch(url, method, headers) {
	val writer = it.bufferedWriter()
	body(writer)
	writer.flush()
}
