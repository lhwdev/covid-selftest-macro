package com.lhwdev.fetch.http

import com.lhwdev.fetch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

private val sHttpProtocols = listOf("http", "https")

fun interface HttpInterceptor : FetchInterceptor {
	override suspend fun intercept(
		url: URL,
		method: FetchMethod?,
		headers: Map<String, String>,
		session: Session?,
		body: FetchBody?
	): FetchResult? {
		if(method !is HttpMethod? || body !is FetchBody?) return null
		if(url.protocol !in sHttpProtocols) return null
		
		return intercept(url, method ?: HttpMethod.get, headers, session, body)
	}
	
	suspend fun intercept(
		url: URL,
		method: HttpMethod,
		headers: Map<String, String>,
		session: Session?,
		body: FetchBody?
	): FetchResult?
}

val HttpInterceptorImpl: HttpInterceptor = HttpInterceptor { url, method, headers, session, body ->
	if(sDebugFetch) {
		println("")
		
		val lastCookieManager = threadLocalCookieHandler
		if(session != null) setThreadLocalCookieHandler(session.cookieManager)
		
		// open
		try {
			val connection = url.openConnection() as HttpURLConnection
			
			println(
				"\u001b[1;91m<- send HTTP \u001B[93m${method}\u001b[0m: ${readableUrl(url.toString())}" +
					if(session == null) "" else " (session)"
			)
			if(body != null) connection.doOutput = true
			
			connection.requestMethod = method.requestName
			
			val previousProperties = connection.requestProperties
			
			for((k, v) in headers) {
				connection.setRequestProperty(k, v)
				println("    \u001B[96m$k\u001B[0m: \u001B[97m$v")
			}
			val contentType = body?.contentType
			if(contentType != null) {
				connection.setRequestProperty("Content-Type", contentType)
				println("    \u001B[96mContent-Type\u001B[0m: \u001B[97m$contentType\u001b[0m (set by HttpBody)")
			}
			
			for((k, v) in previousProperties) {
				println("    \u001B[36m$k\u001B[0m: \u001B[37m$v")
			}
			
			if(session != null) {
				val cookies = session.cookieManager.get(
					url.toURI(), headers.mapValues { listOf(it.value) }
				).values.singleOrNull() // returns "Cookie": [...]
				
				if(cookies?.isNotEmpty() == true) {
					val cookieStr = cookies.fold(cookies.first()) { acc, entry -> "$acc; $entry" }
					println("    \u001B[36mCookie\u001B[0m: \u001B[37m$cookieStr\u001b[0m (session)")
				}
				
				if(session.keepAlive == true) {
					connection.setRequestProperty("Connection", "keep-alive")
					println("    \u001B[36mConnection\u001B[0m: \u001B[37mkeep-alive\u001b[0m (session)")
				}
			}
			
			
			// connect
			connection.connect()
			
			println("\u001B[0m")
			
			if(body != null) connection.outputStream.use {
				if(body.debugAvailable) {
					body.writeDebug(it)
				} else {
					val out = ByteArrayOutputStream()
					body.write(out)
					val array = out.toByteArray()
					System.out.write(array)
					it.write(array)
				}
				println()
			} else {
				println("(no body)")
			}
			
			println()
			
			val response = HttpResultImpl(connection)
			
			println("\u001B[1;91m-> receive \u001B[93m${connection.headerFields[null]!![0]}\u001B[0m")
			println("  \u001B[35m(message: '${connection.responseMessage}')")
			for((k, v) in connection.headerFields) {
				if(k == null) continue
				println("    \u001B[96m$k\u001B[0m: \u001B[97m${v.joinToString()}")
			}
			println("\u001B[0m")
			
			if(response.responseCode in 200..299) {
				val arr = response.rawResponse.readBytes()
				val text = arr.toString(Charsets.UTF_8)
				val count = text.count { it == '\n' }
				if(count < 10) {
					println(text)
				} else {
					val cut = text.splitToSequence('\n')
						.joinToString(limit = 20, separator = "\n", truncated = "\n\u001b[90m  ...\u001b[0m")
					println(cut)
					println()
				}
				object : FetchResult by response {
					override val rawResponse = ByteArrayInputStream(arr)
				}
			} else {
				println("(content: error)")
				response
			}.also {
				println("------------------------")
			}
		} finally {
			if(session != null) setThreadLocalCookieHandler(lastCookieManager)
		}
	} else { // without debug logging
		val lastCookieManager = threadLocalCookieHandler
		if(session != null) setThreadLocalCookieHandler(session.cookieManager)
		
		// open
		try {
			val connection = url.openConnection() as HttpURLConnection
			
			if(body != null) connection.doOutput = true
			
			connection.requestMethod = method.requestName
			
			for((k, v) in headers) {
				connection.setRequestProperty(k, v)
			}
			val contentType = body?.contentType
			if(contentType != null) connection.setRequestProperty("Content-Type", contentType)
			if(session != null) {
				if(session.keepAlive == true) {
					connection.setRequestProperty("Connection", "keep-alive")
				}
			}
			
			// connect
			connection.connect()
			
			if(body != null) connection.outputStream.use { body.write(it) }
			
			val response = HttpResultImpl(connection)
			response.responseCode // preload some so that it utilizes cookie manager
			
			response
		} finally {
			setThreadLocalCookieHandler(lastCookieManager)
		}
	}
	
}

enum class HttpMethod(val requestName: String) : FetchMethod {
	get("GET"),
	head("HEAD"),
	post("POST"),
	delete("DELETE"),
	connect("CONNECT"),
	options("OPTIONS"),
	trace("TRACE"),
	patch("PATCH")
}

private class HttpResultImpl(val connection: HttpURLConnection) :
	FetchResult, Closeable {
	override val responseCode get() = connection.responseCode
	override val responseCodeMessage: String get() = connection.responseMessage
	override val rawResponse: InputStream
		get() {
			requireOk()
			return connection.inputStream
		}
	
	override fun close() {
		connection.disconnect()
	}
}

class HttpIoException(responseCode: Int, responseMessage: String, cause: Throwable? = null) :
	IOException("HTTP error $responseCode $responseMessage", cause)

suspend fun fetch( // for debug
	url: URL,
	method: FetchMethod? = null,
	headers: Map<String, String> = emptyMap(),
	session: Session? = null,
	body: FetchBody? = null
): FetchResult = withContext(Dispatchers.IO) main@{
	for(interceptor in sFetchInterceptors) {
		val result = interceptor.intercept(url, method, headers, session, body)
		if(result != null) return@main result
	}
	error("couldn't find appropriate matcher")
}

suspend fun fetch(
	url: URL,
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	session: Session? = null,
	body: String
): FetchResult = fetch(url, method, headers, session, FetchBody {
	val writer = it.bufferedWriter()
	writer.write(body)
	writer.flush()
})
