package com.lhwdev.io

import com.lhwdev.fetch.StructureBuilder
import kotlinx.serialization.json.*


@StructureBuilder
class JsonObjectScope {
	@PublishedApi
	internal val content: MutableMap<String, JsonElement> = linkedMapOf()
	
	@Suppress("UNUSED_PARAMETER")
	infix fun String.set(value: Nothing?) {
		content[this] = JsonNull
	}
	
	infix fun String.set(value: Number?) {
		content[this] = JsonPrimitive(value)
	}
	
	infix fun String.set(value: String?) {
		content[this] = JsonPrimitive(value)
	}
	
	infix fun String.set(value: Boolean?) {
		content[this] = JsonPrimitive(value)
	}
	
	inline infix fun String.jsonObject(block: JsonObjectScope.() -> Unit) {
		content[this] = com.lhwdev.io.jsonObject(block)
	}
	infix fun String.jsonArray(block: JsonArrayScope.() -> Unit) {
		content[this] = com.lhwdev.io.jsonArray(block)
	}
	
	fun build(): JsonObject = JsonObject(content)
}

@StructureBuilder
class JsonArrayScope {
	@PublishedApi
	internal val content: MutableList<JsonElement> = mutableListOf()
	
	fun add(value: Number) {
		content += JsonPrimitive(value)
	}
	fun add(value: String) {
		content += JsonPrimitive(value)
	}
	fun add(value: Boolean) {
		content += JsonPrimitive(value)
	}
	inline fun addJsonObject(block: JsonObjectScope.() -> Unit) {
		content += jsonObject(block)
	}
	inline fun addJsonArray(block: JsonArrayScope.() -> Unit) {
		content += jsonArray(block) as JsonElement
	}
	
	fun build(): JsonArray = JsonArray(content)
}

inline fun jsonObject(block: JsonObjectScope.() -> Unit): JsonObject =
	JsonObjectScope().apply(block).build()

inline fun jsonArray(block: JsonArrayScope.() -> Unit): JsonArray =
	JsonArrayScope().apply(block).build()

inline fun jsonString(block: JsonObjectScope.() -> Unit): String =
	jsonObject(block).toString()
