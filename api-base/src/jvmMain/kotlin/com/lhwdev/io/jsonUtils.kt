package com.lhwdev.io

import com.lhwdev.fetch.StructureBuilder
import kotlinx.serialization.json.*
import kotlin.native.concurrent.SharedImmutable


@StructureBuilder
abstract class JsonObjectScope {
	@Suppress("UNUSED_PARAMETER")
	abstract infix fun String.set(value: Nothing?)
	
	abstract infix fun String.set(value: Number?)
	
	abstract infix fun String.set(value: String?)
	
	abstract infix fun String.set(value: Boolean?)
	
	inline infix fun String.jsonObject(block: JsonObjectScope.() -> Unit) {
		jsonObject(this, jsonObjectScope(this).apply(block))
	}
	
	abstract fun jsonObjectScope(key: String): JsonObjectScope
	
	abstract fun jsonObject(key: String, scope: JsonObjectScope)
	
	inline infix fun String.jsonArray(block: JsonArrayScope.() -> Unit) {
		jsonArray(this, jsonArrayScope(this).apply(block))
	}
	
	abstract fun jsonArrayScope(key: String): JsonArrayScope
	
	abstract fun jsonArray(key: String, scope: JsonArrayScope)
}

@StructureBuilder
abstract class JsonArrayScope {
	abstract fun add(value: Nothing?)
	
	abstract fun add(value: Number?)
	
	abstract fun add(value: String?)
	
	abstract fun add(value: Boolean?)
	
	inline fun addObject(block: JsonObjectScope.() -> Unit) {
		addObject(jsonObjectScope().apply(block))
	}
	
	abstract fun jsonObjectScope(): JsonObjectScope
	
	abstract fun addObject(scope: JsonObjectScope)
	
	inline fun addArray(block: JsonArrayScope.() -> Unit) {
		addArray(jsonArrayScope().apply(block))
	}
	
	abstract fun jsonArrayScope(): JsonArrayScope
	
	abstract fun addArray(scope: JsonArrayScope)
}


// to object

class JsonObjectScopeImpl : JsonObjectScope() {
	@PublishedApi
	internal val content: MutableMap<String, JsonElement> = linkedMapOf()
	
	@Suppress("UNUSED_PARAMETER")
	override infix fun String.set(value: Nothing?) {
		content[this] = JsonNull
	}
	
	override infix fun String.set(value: Number?) {
		content[this] = JsonPrimitive(value)
	}
	
	override infix fun String.set(value: String?) {
		content[this] = JsonPrimitive(value)
	}
	
	override infix fun String.set(value: Boolean?) {
		content[this] = JsonPrimitive(value)
	}
	
	override fun jsonObjectScope(key: String): JsonObjectScopeImpl = JsonObjectScopeImpl()
	
	override fun jsonObject(key: String, scope: JsonObjectScope) {
		require(scope is JsonObjectScopeImpl)
		content[key] = scope.build()
	}
	
	override fun jsonArrayScope(key: String): JsonArrayScope = JsonArrayScopeImpl()
	
	override fun jsonArray(key: String, scope: JsonArrayScope) {
		require(scope is JsonArrayScopeImpl)
		content[key] = scope.build()
	}
	
	fun build(): JsonObject = JsonObject(content)
}

class JsonArrayScopeImpl : JsonArrayScope() {
	@PublishedApi
	internal val content: MutableList<JsonElement> = mutableListOf()
	
	override fun add(value: Nothing?) {
		content += JsonPrimitive(null as String?)
	}
	
	override fun add(value: Number?) {
		content += JsonPrimitive(value)
	}
	
	override fun add(value: String?) {
		content += JsonPrimitive(value)
	}
	
	override fun add(value: Boolean?) {
		content += JsonPrimitive(value)
	}
	
	override fun jsonObjectScope(): JsonObjectScope = JsonObjectScopeImpl()
	
	override fun addObject(scope: JsonObjectScope) {
		require(scope is JsonObjectScopeImpl)
		content += scope.build()
	}
	
	override fun jsonArrayScope(): JsonArrayScope = JsonArrayScopeImpl()
	
	override fun addArray(scope: JsonArrayScope) {
		require(scope is JsonArrayScopeImpl)
		content += scope.build()
	}
	
	fun build(): JsonArray = JsonArray(content)
}


private fun toHexChar(i: Int): Char {
	val d = i and 0xf
	return if(d < 10) (d + '0'.code).toChar()
	else (d - 10 + 'a'.code).toChar()
}

private const val NULL = "null"

@SharedImmutable
internal val ESCAPE_STRINGS: Array<String?> = arrayOfNulls<String>(93).apply {
	for(c in 0..0x1f) {
		val c1 = toHexChar(c shr 12)
		val c2 = toHexChar(c shr 8)
		val c3 = toHexChar(c shr 4)
		val c4 = toHexChar(c)
		this[c] = "\\u$c1$c2$c3$c4"
	}
	this['"'.code] = "\\\""
	this['\\'.code] = "\\\\"
	this['\t'.code] = "\\t"
	this['\b'.code] = "\\b"
	this['\n'.code] = "\\n"
	this['\r'.code] = "\\r"
	this[0x0c] = "\\f"
}

@SharedImmutable
internal val ESCAPE_MARKERS: ByteArray = ByteArray(93).apply {
	for(c in 0..0x1f) {
		this[c] = 1.toByte()
	}
	this['"'.code] = '"'.code.toByte()
	this['\\'.code] = '\\'.code.toByte()
	this['\t'.code] = 't'.code.toByte()
	this['\b'.code] = 'b'.code.toByte()
	this['\n'.code] = 'n'.code.toByte()
	this['\r'.code] = 'r'.code.toByte()
	this[0x0c] = 'f'.code.toByte()
}

internal fun StringBuilder.printQuoted(value: String) {
	append('"')
	var lastPos = 0
	for(i in value.indices) {
		val c = value[i].code
		if(c < ESCAPE_STRINGS.size && ESCAPE_STRINGS[c] != null) {
			append(value, lastPos, i) // flush prev
			append(ESCAPE_STRINGS[c])
			lastPos = i + 1
		}
	}
	
	if(lastPos != 0) append(value, lastPos, value.length)
	else append(value)
	append('"')
}

class JsonObjectScopeToString(val builder: StringBuilder) : JsonObjectScope() {
	private var isFirst = true
	
	
	private fun entry(key: String) {
		if(!isFirst) {
			builder.append(',')
		}
		
		builder.printQuoted(key)
		builder.append(':')
		
		isFirst = false
	}
	
	private fun entry(key: String, value: String) {
		entry(key)
		builder.append(value)
	}
	
	fun openBracket() {
		builder.append('{')
	}
	
	fun closeBracket() {
		builder.append('}')
	}
	
	override fun String.set(value: Nothing?) {
		entry(this, NULL)
	}
	
	override fun String.set(value: Number?) {
		entry(this, if(value == null) NULL else value.toString())
	}
	
	override fun String.set(value: Boolean?) {
		entry(this, if(value == null) NULL else value.toString())
	}
	
	override fun String.set(value: String?) {
		entry(this)
		if(value == null) {
			builder.append(NULL)
		} else {
			builder.printQuoted(value)
		}
	}
	
	override fun jsonObjectScope(key: String): JsonObjectScope {
		entry(key)
		return JsonObjectScopeToString(builder = builder).also { it.openBracket() }
	}
	
	override fun jsonObject(key: String, scope: JsonObjectScope) {
		require(scope is JsonObjectScopeToString)
		scope.closeBracket()
	}
	
	override fun jsonArrayScope(key: String): JsonArrayScope {
		entry(key)
		return JsonArrayScopeToString(builder = builder).also { it.openBracket() }
	}
	
	override fun jsonArray(key: String, scope: JsonArrayScope) {
		require(scope is JsonArrayScopeToString)
		scope.closeBracket()
	}
	
	fun build(): String {
		closeBracket()
		return builder.toString()
	}
}

class JsonArrayScopeToString(val builder: StringBuilder) : JsonArrayScope() {
	private var isFirst = true
	
	private fun entry() {
		if(!isFirst) {
			builder.append(',')
		}
		isFirst = true
	}
	
	private fun entry(value: String) {
		entry()
		builder.append(value)
	}
	
	fun openBracket() {
		builder.append('[')
	}
	
	fun closeBracket() {
		builder.append(']')
	}
	
	override fun add(value: Nothing?) {
		entry(NULL)
	}
	
	override fun add(value: Number?) {
		entry(if(value == null) NULL else value.toString())
	}
	
	override fun add(value: String?) {
		entry()
		if(value == null) {
			builder.append(NULL)
		} else {
			builder.printQuoted(value)
		}
	}
	
	override fun add(value: Boolean?) {
		entry(if(value == null) NULL else value.toString())
	}
	
	override fun jsonObjectScope(): JsonObjectScope {
		entry()
		return JsonObjectScopeToString(builder = builder).also { it.openBracket() }
	}
	
	override fun addObject(scope: JsonObjectScope) {
		require(scope is JsonObjectScopeToString)
		scope.closeBracket()
	}
	
	override fun jsonArrayScope(): JsonArrayScope {
		entry()
		return JsonArrayScopeToString(builder = builder).also { it.openBracket() }
	}
	
	override fun addArray(scope: JsonArrayScope) {
		require(scope is JsonArrayScopeToString)
		scope.closeBracket()
	}
	
	fun build(): String {
		closeBracket()
		return builder.toString()
	}
}

inline fun jsonObject(block: JsonObjectScopeImpl.() -> Unit): JsonObject =
	JsonObjectScopeImpl().apply(block).build()

inline fun jsonArray(block: JsonArrayScopeImpl.() -> Unit): JsonArray =
	JsonArrayScopeImpl().apply(block).build()

fun jsonObjectStringScope(): JsonObjectScopeToString =
	JsonObjectScopeToString(builder = StringBuilder()).also { it.openBracket() }

inline fun jsonObjectString(block: JsonObjectScopeToString.() -> Unit): String =
	jsonObjectStringScope().apply(block).build()

fun jsonArrayStringScope(): JsonArrayScopeToString =
	JsonArrayScopeToString(builder = StringBuilder()).also { it.openBracket() }

inline fun jsonArrayString(block: JsonArrayScopeToString.() -> Unit): String =
	jsonArrayStringScope().apply(block).build()
