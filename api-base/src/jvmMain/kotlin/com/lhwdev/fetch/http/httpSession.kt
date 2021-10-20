package com.lhwdev.fetch.http

import com.lhwdev.fetch.*
import java.net.*


private val sCookieThreadLocal = ThreadLocal<CookieHandler?>()


object ThreadLocalCookieManager : CookieHandler() {
	override fun get(
		uri: URI,
		requestHeaders: Map<String, List<String>>
	): Map<String, List<String>> = sCookieThreadLocal.get()?.get(uri, requestHeaders) ?: emptyMap()
	
	override fun put(uri: URI, responseHeaders: Map<String, List<String>>) {
		sCookieThreadLocal.get()?.put(uri, responseHeaders)
	}
}

fun setThreadLocalCookieHandler(handler: CookieHandler?) {
	sCookieThreadLocal.set(handler)
}

val threadLocalCookieHandler: CookieHandler? get() = sCookieThreadLocal.get()


@Suppress("unused")
private val sDummyInitialization = run {
	CookieHandler.setDefault(ThreadLocalCookieManager)
}


class Session(
	val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL),
	var keepAlive: Boolean? = null
)


suspend inline fun Session.fetch(
	url: URL,
	method: FetchMethod? = null,
	headers: FetchHeadersBuilder,
	body: FetchBody? = null
): FetchResult = fetch(url, method, headers, this, body)

suspend fun Session.fetch(
	url: URL,
	method: FetchMethod? = null,
	headers: Map<String, String> = emptyMap(),
	body: FetchBody? = null
): FetchResult = fetch(url, method, headers, this, body)

suspend inline fun Session.fetch(
	url: String,
	method: FetchMethod? = null, headers: FetchHeadersBuilder,
	body: FetchBody? = null
): FetchResult = fetch(url, method, headers, this, body)

suspend inline fun Session.fetch(
	url: String,
	method: FetchMethod? = null, headers: Map<String, String> = emptyMap(),
	body: FetchBody? = null
): FetchResult = fetch(url, method, headers, this, body)
