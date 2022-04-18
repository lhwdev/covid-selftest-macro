@file:Suppress("BlockingMethodInNonBlockingContext")

package com.lhwdev.fetch

import com.lhwdev.fetch.headers.ContentType
import com.lhwdev.fetch.http.HttpInterceptorImpl
import com.lhwdev.fetch.http.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder
import java.util.LinkedList
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


interface FetchRequest {
	val url: URL
	val method: FetchMethod?
	val headers: Map<String, String>
	val session: Session?
	val body: FetchBody?
	
	fun dump(): String {
		var text = "$method $url headers=(${headers.entries.joinToString { (k, v) -> "'$k'='$v'" }})"
		if(session != null) text += " session=$session"
		if(body != null) text += " body=${body?.dump()}"
		return text
	}
}

class FetchRequestImpl(
	override val url: URL,
	override val method: FetchMethod?,
	override val headers: Map<String, String>,
	override val session: Session?,
	override val body: FetchBody?
) : FetchRequest


fun interface FetchInterceptor {
	suspend fun intercept(request: FetchRequest, interceptorChain: InterceptorChain): FetchResult?
}

suspend inline fun FetchInterceptor.ensureIntercepted(
	request: FetchRequest,
	interceptorChain: InterceptorChain
): FetchResult = intercept(request, interceptorChain)
	?: interceptorChain.interceptNext(request)

class InterceptorChain(private val list: List<FetchInterceptor>, private val index: Int) {
	suspend fun interceptNext(request: FetchRequest): FetchResult {
		return nextInterceptor().ensureIntercepted(request, nextChain())
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
private val sFetchInterceptorChain = InterceptorChain(sFetchInterceptors, 0)

class FetchInterceptorContext(val interceptorChain: InterceptorChain) :
	AbstractCoroutineContextElement(FetchInterceptorContext), CoroutineContext.Element {
	override val key = Key
	
	companion object Key : CoroutineContext.Key<FetchInterceptorContext>
}


interface FetchBody {
	fun dump(): String = toString()
}

interface DataBody : FetchBody {
	fun write(out: OutputStream)
	
	fun writeDebug(out: OutputStream): Unit = error("debug not capable")
	val debugAvailable: Boolean get() = false
	val contentType: ContentType?
}

inline fun DataBody(contentType: ContentType?, crossinline onWrite: (OutputStream) -> Unit): DataBody =
	object : DataBody {
		override fun write(out: OutputStream) {
			onWrite(out)
		}
		
		override val contentType: ContentType? = contentType
	}


interface FetchMethod


interface FetchHeader {
	fun serialize(): String
}

abstract class FetchHeaderKey<T : FetchHeader>(val key: String) {
	abstract fun parse(value: String): T
}

interface FetchHeaders {
	operator fun <T : FetchHeader> get(key: FetchHeaderKey<T>): T?
	operator fun get(key: String): String?
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
	override fun get(key: String): String? =
		stringMap[key] ?: map.entries.find { it.key.key == key }?.value?.serialize()
	
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
	request: FetchRequest, // note: potential memory leakage
	val responseCode: Int,
	val responseMessage: String,
	cause: Throwable? = null
) : IOException("Fetch error $responseCode $responseMessage\nRequest: ${request.dump()}", cause)


var sDebugFetch = false


internal fun readableUrl(url: String): String {
	val index = url.indexOf("?")
	if(index == -1) return url
	val link = url.substring(0, index)
	val attrs = url.substring(index + 1)
	return "$link?.. (urlParams: " + attrs.split('&').joinToString {
		val eqIndex = it.indexOf('=')
		val k = it.substring(0, eqIndex)
		val v = URLDecoder.decode(it.substring(eqIndex + 1), "UTF-8")
		"$k='$v'"
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

suspend inline fun fetch(
	url: URL,
	method: FetchMethod? = null,
	headers: Map<String, String> = emptyMap(),
	session: Session? = null,
	body: FetchBody? = null
): FetchResult = fetch(FetchRequestImpl(url, method, headers, session, body))

suspend fun fetch(
	request: FetchRequest
): FetchResult = withContext(Dispatchers.IO) {
	val chain = coroutineContext[FetchInterceptorContext]?.interceptorChain ?: sFetchInterceptorChain
	
	chain.interceptNext(request)
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
