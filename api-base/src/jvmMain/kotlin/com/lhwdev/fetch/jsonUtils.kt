package com.lhwdev.fetch

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


@StructureBuilder
class JsonObjectScope {
	@PublishedApi
	internal val content: MutableMap<String, JsonElement> = linkedMapOf()
	
	infix fun String.set(value: Number) {
		content[this] = JsonPrimitive(value)
	}
	infix fun String.set(value: String) {
		content[this] = JsonPrimitive(value)
	}
	infix fun String.set(value: Boolean) {
		content[this] = JsonPrimitive(value)
	}
	inline infix fun String.jsonObject(block: JsonObjectScope.() -> Unit) {
		content[this] = com.lhwdev.fetch.jsonObject(block)
	}
	infix fun String.jsonArray(block: JsonArrayScope.() -> Unit) {
		content[this] = com.lhwdev.fetch.jsonArray(block)
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
