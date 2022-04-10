@file:Suppress("BlockingMethodInNonBlockingContext")

package com.lhwdev.fetch.http

import com.lhwdev.fetch.*
import com.lhwdev.io.runInterruptibleGracefully
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


private val sHttpProtocols = listOf("http", "https")

interface HttpRequest : FetchRequest {
	override val method: HttpMethod
	override val body: DataBody?
}

class HttpRequestImpl(
	override val url: URL,
	override val method: HttpMethod,
	override val headers: Map<String, String>,
	override val session: Session?,
	override val body: DataBody?
) : HttpRequest

fun interface HttpInterceptor : FetchInterceptor {
	override suspend fun intercept(request: FetchRequest, interceptorChain: InterceptorChain): FetchResult? {
		val method = request.method
		val body = request.body
		if(method !is HttpMethod? || body !is DataBody?) return null
		if(request.url.protocol !in sHttpProtocols) return null
		
		return intercept(
			HttpRequestImpl(
				url = request.url,
				method = when {
					method != null -> method
					body == null -> HttpMethod.get // following common behavior
					else -> HttpMethod.post
				},
				headers = request.headers,
				session = request.session,
				body = body
			),
			interceptorChain
		)
	}
	
	suspend fun intercept(request: HttpRequest, interceptorChain: InterceptorChain): FetchResult?
}


object HttpInterceptorImpl : HttpInterceptor {
	override suspend fun intercept(request: HttpRequest, interceptorChain: InterceptorChain): FetchResult? {
		val url = request.url
		val method = request.method
		val headers = request.headers
		val session = request.session
		val body = request.body
		
		val cookieManager = session?.cookieManager
		return if(sDebugFetch) runInterruptibleGracefully(Dispatchers.IO) {
			println("")
			
			val lastCookieManager = threadLocalCookieHandler
			if(cookieManager != null) setThreadLocalCookieHandler(cookieManager)
			
			// open
			try {
				val connection = url.openConnection() as HttpURLConnection
				
				println(
					"<- send HTTP ${method}: ${readableUrl(url.toString())}" +
						if(session == null) "" else " (session)"
				)
				if(body != null) connection.doOutput = true
				
				connection.requestMethod = method.requestName
				
				val previousProperties = connection.requestProperties
				
				for((k, v) in headers) {
					connection.setRequestProperty(k, v)
					println("    $k: $v")
				}
				val contentType = body?.contentType
				if(contentType != null) {
					connection.setRequestProperty("Content-Type", contentType.serialize())
					println("    Content-Type: $contentType (set by HttpBody)")
				}
				
				for((k, v) in previousProperties) {
					println("    $k: $v")
				}
				
				if(session != null) {
					val cookies = cookieManager?.get(
						url.toURI(), headers.mapValues { listOf(it.value) }
					)?.values?.singleOrNull() // returns "Cookie": [...]
					
					if(cookies?.isNotEmpty() == true) {
						val cookieStr = cookies.fold(cookies.first()) { acc, entry -> "$acc; $entry" }
						println("    Cookie: $cookieStr (session)")
					}
					
					if(session.keepAlive == true) {
						connection.setRequestProperty("Connection", "keep-alive")
						println("    Connection: keep-alive (session)")
					}
				}
				
				
				// connect
				connection.connect()
				
				println("")
				
				if(body != null) connection.outputStream.use {
					if(body.debugAvailable) {
						body.writeDebug(it)
					} else {
						val out = ByteArrayOutputStream()
						body.write(out)
						val array = out.toByteArray()
						System.out.write(array, 0, array.size.coerceAtMost(2000))
						it.write(array)
					}
					println()
				} else {
					println("(no body)")
				}
				
				println()
				
				val response = HttpResultImpl(request, connection)
				
				println("-> receive ${connection.headerFields[null]!![0]}")
				println("  (message: '${connection.responseMessage}')")
				for((k, v) in connection.headerFields) {
					if(k == null) continue
					println("    $k: ${v.joinToString()}")
				}
				println("")
				
				if(response.responseCode in 200..299) {
					val arr = response.rawResponse.readBytes()
					val text = arr.toString(Charsets.UTF_8)
					val count = text.count { it == '\n' }
					if(count < 10) {
						println(text)
					} else {
						val cut = text.let { if(it.length > 2000) it.take(2000) else it }
							.splitToSequence('\n')
							.joinToString(limit = 20, separator = "\n", truncated = "...")
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
		} else runInterruptibleGracefully(Dispatchers.IO) { // without debug logging
			val lastCookieManager = threadLocalCookieHandler
			if(cookieManager != null) setThreadLocalCookieHandler(cookieManager)
			
			// open
			try {
				val connection = url.openConnection() as HttpURLConnection
				
				if(body != null) connection.doOutput = true
				
				connection.requestMethod = method.requestName
				
				for((k, v) in headers) {
					connection.setRequestProperty(k, v)
				}
				val contentType = body?.contentType
				if(contentType != null) connection.setRequestProperty("Content-Type", contentType.serialize())
				if(session != null) {
					if(session.keepAlive == true) {
						connection.setRequestProperty("Connection", "keep-alive")
					}
				}
				
				// connect
				connection.connect()
				
				if(body != null) connection.outputStream.use { body.write(it) }
				val response = HttpResultImpl(request, connection)
				response.responseCode // preload some so that it utilizes cookie manager
				
				response
			} finally {
				if(session != null) setThreadLocalCookieHandler(lastCookieManager)
			}
		}
	}
	
	override fun toString(): String = "HTTP"
}


@Suppress("unused")
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


private class HttpResultImpl(override val request: FetchRequest, val connection: HttpURLConnection) : FetchResult {
	override val responseCode get() = connection.responseCode
	override val responseCodeMessage: String get() = connection.responseMessage
	override val rawResponse: InputStream
		get() {
			requireOk()
			return connection.inputStream
		}
	
	override suspend fun close(): Unit = runInterruptibleGracefully(Dispatchers.IO) {
		connection.disconnect()
	}
	
	
	// fetch headers
	private val cache = mutableMapOf<FetchHeaderKey<*>, FetchHeader?>()
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : FetchHeader> get(key: FetchHeaderKey<T>): T? = cache.getOrPut(key) {
		connection.getHeaderField(key.key)?.let { key.parse(it) }
	} as T?
	
	override fun contains(key: FetchHeaderKey<*>): Boolean = get(key) != null
	override fun contains(key: String): Boolean = connection.getHeaderField(key) != null
}
