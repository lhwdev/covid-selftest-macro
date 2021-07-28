package com.lhwdev.selfTestMacro

import java.net.*


private val sCookieThreadLocal = ThreadLocal<CookieHandler>()


object ThreadLocalCookieManager : CookieHandler() {
	override fun get(
		uri: URI,
		requestHeaders: Map<String, List<String>>
	): Map<String, List<String>> = sCookieThreadLocal.get()?.get(uri, requestHeaders) ?: emptyMap()
	
	override fun put(uri: URI, responseHeaders: Map<String, List<String>>) {
		sCookieThreadLocal.get()?.put(uri, responseHeaders)
	}
}

fun setThreadLocalCookieHandler(handler: CookieHandler) {
	sCookieThreadLocal.set(handler)
}


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
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	body: HttpBody? = null
): FetchResult = fetch(url, method, headers, this, body)

suspend inline fun Session.fetch(
	url: URL,
	method: HttpMethod = HttpMethod.get,
	headers: Map<String, String> = emptyMap(),
	body: String
): FetchResult = fetch(url, method, headers, this, body)
