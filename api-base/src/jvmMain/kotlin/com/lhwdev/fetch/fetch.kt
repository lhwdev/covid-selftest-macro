@file:Suppress("BlockingMethodInNonBlockingContext")

package com.lhwdev.fetch

import com.lhwdev.fetch.http.HttpInterceptorImpl
import com.lhwdev.fetch.http.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder
import java.util.LinkedList


fun interface FetchInterceptor {
	suspend fun intercept(
		url: URL,
		method: FetchMethod?, headers: Map<String, String>,
		session: Session?, body: FetchBody?,
		interceptorChain: InterceptorChain
	): FetchResult?
}

suspend inline fun FetchInterceptor.ensureIntercepted(
	url: URL,
	method: FetchMethod?, headers: Map<String, String>,
	session: Session?, body: FetchBody?,
	interceptorChain: InterceptorChain
): FetchResult = intercept(url, method, headers, session, body, interceptorChain)
	?: interceptorChain.interceptNext(url, method, headers, session, body)

class InterceptorChain(private val list: List<FetchInterceptor>, private val index: Int) {
	suspend fun interceptNext(
		url: URL,
		method: FetchMethod?, headers: Map<String, String>,
		session: Session?, body: FetchBody?
	): FetchResult {
		return nextInterceptor().ensureIntercepted(url, method, headers, session, body, nextChain())
	}
	
	fun nextChain(): InterceptorChain {
		check(index + 1 in list.indices)
		return InterceptorChain(list, index + 1)
	}
	
	fun nextInterceptorOrNull(): FetchInterceptor? =
		list.getOrNull(index + 1)
	
	fun nextInterceptor(): FetchInterceptor =
		nextInterceptorOrNull() ?: error("couldn't find appropriate interceptor")
}



val sFetchInterceptors = LinkedList<FetchInterceptor>(listOf(HttpInterceptorImpl))


interface FetchBody

interface DataBody : FetchBody {
	fun write(out: OutputStream)
	
	fun writeDebug(out: OutputStream): Unit = error("debug not capable")
	val debugAvailable: Boolean get() = false
	val contentType: String?
}

inline fun DataBody(contentType: String?, crossinline onWrite: (OutputStream) -> Unit): DataBody = object : DataBody {
	override fun write(out: OutputStream) {
		onWrite(out)
	}
	
	override val contentType: String?
		get() = contentType
}


interface FetchMethod


interface FetchHeader {
	fun serialize(): String
}

interface FetchHeaderKey<T : FetchHeader> {
	val key: String
	fun parse(value: String): T
}

inline fun <T : FetchHeader> FetchHeaderKey(key: String, crossinline parse: (String) -> T): FetchHeaderKey<T> =
	object : FetchHeaderKey<T> {
		override val key: String = key
		override fun parse(value: String): T = parse(value)
	}

interface FetchHeaders {
	operator fun <T : FetchHeader> get(key: FetchHeaderKey<T>): T?
	operator fun contains(key: FetchHeaderKey<*>): Boolean
	operator fun contains(key: String): Boolean
}

interface MutableFetchHeaders : FetchHeaders {
	operator fun <T : FetchHeader> set(key: FetchHeaderKey<T>, value: T?)
}

@Suppress("UNCHECKED_CAST")
class MutableFetchHeadersBuilder : MutableFetchHeaders {
	private val map = mutableMapOf<FetchHeaderKey<*>, FetchHeader>()
	private val stringMap = mutableMapOf<String, String>()
	
	override fun <T : FetchHeader> get(key: FetchHeaderKey<T>): T? = map[key] as T?
	override fun contains(key: FetchHeaderKey<*>): Boolean = key in map || key.key in stringMap
	override fun contains(key: String): Boolean = map.keys.any { it.key == key } || key in stringMap
	override fun <T : FetchHeader> set(key: FetchHeaderKey<T>, value: T?) {
		if(value == null) {
			map -= key
		} else {
			map[key] = value
		}
	}
	
	operator fun set(key: String, value: String) {
		stringMap[key] = value
	}
	
	@PublishedApi
	internal fun build(): Map<String, String> {
		for((key, value) in map) {
			val realKey = key.key
			if(realKey in stringMap) error("duplicated key $realKey")
			stringMap[realKey] = value.serialize()
		}
		return stringMap
	}
}


@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class FetchIoException(
	val debugInfo: String,
	val interceptorDescription: String,
	val responseCode: Int,
	val responseMessage: String,
	cause: Throwable? = null
) : IOException("Fetch error($interceptorDescription: $debugInfo) $responseCode $responseMessage", cause)


var sDebugFetch = false


internal fun readableUrl(url: String): String {
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

typealias FetchHeadersBuilderBlock = MutableFetchHeadersBuilder.() -> Unit

suspend inline fun fetch(
	url: URL,
	method: FetchMethod? = null,
	headers: FetchHeadersBuilderBlock,
	session: Session? = null,
	body: FetchBody? = null
): FetchResult = fetch(url, method, MutableFetchHeadersBuilder().apply(headers).build(), session, body)

suspend fun fetch(
	url: URL,
	method: FetchMethod? = null,
	headers: Map<String, String> = emptyMap(),
	session: Session? = null,
	body: FetchBody? = null
): FetchResult = withContext(Dispatchers.IO) main@{
	val interceptor = sFetchInterceptors.firstOrNull() ?: error("no interceptors")
	interceptor.ensureIntercepted(url, method, headers, session, body, InterceptorChain(sFetchInterceptors, 0))
}

suspend inline fun fetch(
	url: String,
	method: FetchMethod? = null, headers: FetchHeadersBuilderBlock,
	session: Session? = null, body: FetchBody? = null
): FetchResult = fetch(URL(url), method, headers, session, body)

suspend inline fun fetch(
	url: String,
	method: FetchMethod? = null, headers: Map<String, String> = emptyMap(),
	session: Session? = null, body: FetchBody? = null
): FetchResult = fetch(URL(url), method, headers, session, body)
