@file:Suppress("BlockingMethodInNonBlockingContext")

package com.lhwdev.fetch

import com.lhwdev.fetch.headers.ContentType
import com.lhwdev.fetch.headers.ContentTypes
import com.lhwdev.fetch.headers.contentType
import com.lhwdev.io.runInterruptibleGracefully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.charset.Charset


interface FetchResult : FetchHeaders {
	val request: FetchRequest
	
	val responseCode: Int
	val responseCodeMessage: String
	val rawResponse: InputStream
	
	suspend fun close()
}

suspend inline fun <R> FetchResult.use(block: (FetchResult) -> R): R = try {
	block(this)
} finally {
	close()
}


val FetchResult.isOk: Boolean get() = responseCode in 200..299
fun FetchResult.requireOk() {
	if(!isOk) throw FetchIoException(request, responseCode, responseCodeMessage)
}

val FetchResult.charset: Charset get() = contentType?.charset ?: Charsets.UTF_8

fun FetchResult.assertContentTypeCompatible(type: ContentType, anyContentType: Boolean = false) {
	check(anyContentType || contentType?.isCompatible(type) == true) {
		"Content-Type of fetched resource is not ${type.mediaType}: $contentType"
	}
}

suspend fun <T> FetchResult.toJson(
	serializer: KSerializer<T>,
	from: Json = Json,
	charset: Charset = this.charset,
	anyContentType: Boolean = false
): T = withContext(Dispatchers.Default) {
	assertContentTypeCompatible(ContentTypes.json, anyContentType)
	from.decodeFromString(serializer, getText(charset))
}

suspend fun FetchResult.getText(charset: Charset = this.charset): String = use {
	runInterruptibleGracefully(Dispatchers.IO) {
		val value = rawResponse.reader(charset = charset).readText()
		rawResponse.close()
		value
	}
}


// suspend fun FetchResult.toJson(
// 	from: Json = Json,
// 	charset: Charset = this.charset
// ): JsonElement = withContext(Dispatchers.IO) {
// 	from.parseToJsonElement(getText(charset))
// }
