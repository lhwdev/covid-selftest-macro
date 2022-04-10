@file:Suppress("unused", "BlockingMethodInNonBlockingContext")

package com.lhwdev.fetch

import com.lhwdev.fetch.headers.ContentType
import com.lhwdev.fetch.headers.ContentTypes
import com.lhwdev.io.JsonObjectScope
import com.lhwdev.io.JsonObjectScopeImpl
import com.lhwdev.io.JsonObjectScopeToString
import com.lhwdev.io.jsonObjectStringScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import java.io.OutputStream
import java.io.Writer


@DslMarker
@Retention(AnnotationRetention.SOURCE)
annotation class StructureBuilder


object Bodies


interface WriterDataBody : DataBody {
	override fun write(out: OutputStream) {
		val writer = out.writer()
		write(writer)
		writer.flush()
		writer.close()
	}
	
	fun write(out: Writer)
	
	override fun writeDebug(out: OutputStream) {
		val writer = out.writer()
		val debug = writeDebug(writer)
		writer.flush()
		writer.close()
		return debug
	}
	
	fun writeDebug(out: Writer): Unit = error("debug not provided")
}

inline fun WriterDataBody(
	contentType: ContentType?,
	crossinline onWrite: (Writer) -> Unit
): WriterDataBody = object : WriterDataBody {
	override fun write(out: Writer) {
		onWrite(out)
	}
	
	override val contentType: ContentType?
		get() = contentType
}



fun Bodies.binary(array: ByteArray): DataBody = DataBody(contentType = ContentTypes.binary) { it.write(array) }
fun Bodies.text(text: String): DataBody = WriterDataBody(contentType = ContentTypes.plainText) { it.write(text) }

fun <T> Bodies.json(serializer: KSerializer<T>, value: T, json: Json = Json): DataBody = object : WriterDataBody {
	override fun write(out: Writer) {
		out.write(json.encodeToString(serializer, value))
	}
	
	override val contentType: ContentType get() = ContentTypes.json
	
	override fun writeDebug(out: Writer) {
		val config = Json(from = json) { prettyPrint = true }
		val result = config.encodeToString(serializer, value)
		out.write(result)
		println(result)
	}
	
	override fun dump(): String {
		val config = Json(from = json) { prettyPrint = true }
		return config.encodeToString(serializer, value)
	}
	
	override val debugAvailable: Boolean get() = true
}


fun Bodies.jsonObject(json: JsonObject): DataBody = object : WriterDataBody {
	override fun write(out: Writer) {
		out.write(json.toString())
	}
	
	override val contentType: ContentType get() = ContentTypes.json
	
	override val debugAvailable: Boolean get() = true
	override fun writeDebug(out: Writer) {
		out.write(json.toString())
		fun dump(json: JsonElement, indent: String): Unit = when(json) {
			JsonNull -> println("null")
			is JsonObject -> {
				println("{")
				for((key, value) in json.entries) {
					print("$indent  ")
					print("\"$key\": ")
					dump(value, "$indent    ")
				}
				println("$indent}")
			}
			is JsonArray -> {
				println("[")
				for(value in json) {
					print("$indent  ")
					dump(value, "$indent    ")
				}
				println("$indent]")
			}
			is JsonPrimitive -> {
				println("$json")
			}
		}
		dump(json, "")
	}
	
	override fun dump(): String = json.toString()
}

fun Bodies.jsonObject(json: String): DataBody = WriterDataBody(contentType = ContentTypes.json) {
	it.write(json)
}

@PublishedApi
internal fun jsonObjectOrStringScope(): JsonObjectScope = if(sDebugFetch) {
	JsonObjectScopeImpl()
} else {
	jsonObjectStringScope()
}

@PublishedApi
internal fun Bodies.jsonObject(scope: JsonObjectScope): DataBody = when(scope) {
	is JsonObjectScopeImpl -> jsonObject(scope.build())
	is JsonObjectScopeToString -> jsonObject(scope.build())
}


inline fun Bodies.jsonObject(block: JsonObjectScope.() -> Unit): DataBody {
	val scope = jsonObjectOrStringScope()
	scope.block()
	return jsonObject(scope)
}

@PublishedApi
internal fun Bodies.form(scope: FormScope): DataBody = object : WriterDataBody {
	override fun write(out: Writer) {
		out.write(scope.build())
	}
	
	override val contentType: ContentType get() = ContentTypes.form
	
	override fun writeDebug(out: Writer) {
		out.write(scope.build())
		println("(html form)")
		for((key, value) in scope.content) {
			println("$key=$value")
		}
	}
	
	override fun dump(): String = "form " + scope.content.entries.joinToString { (k, v) -> "$k=$v" }
	
	override val debugAvailable: Boolean get() = true
}

fun Bodies.form(block: FormScope.() -> Unit): DataBody =
	form(FormScope().apply(block))

@StructureBuilder
class FormScope {
	val content = mutableMapOf<String, String>()
	
	infix fun String.set(value: String) {
		content[this] = value
	}
	
	fun build(): String = queryUrlParamsToString(content)
}
