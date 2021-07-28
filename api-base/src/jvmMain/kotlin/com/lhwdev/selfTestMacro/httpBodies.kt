@file:Suppress("unused")

package com.lhwdev.selfTestMacro

import kotlinx.serialization.json.*
import java.io.OutputStream
import java.io.Writer


@DslMarker
@Retention(AnnotationRetention.SOURCE)
annotation class StructureBuilder


object HttpBodies


interface WriterHttpBody : HttpBody {
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


fun HttpBodies.jsonObject(block: JsonObjectScope.() -> Unit): HttpBody = object : WriterHttpBody {
	override fun write(out: Writer) {
		out.write(jsonString(block))
	}
	
	override val contentType: String get() = "application/json"
	
	override val debugAvailable: Boolean get() = true
	override fun writeDebug(out: Writer) {
		val json = com.lhwdev.selfTestMacro.jsonObject(block)
		out.write(json.toString())
		fun dump(json: JsonElement, indent: String): Unit = when(json) {
			JsonNull -> println("\u001b[96mnull\u001b[0m")
			is JsonObject -> {
				println("\u001b[96m{")
				for((key, value) in json.entries) {
					print("$indent  ")
					print("\u001b[91m\"$key\"\u001b[0m: ")
					dump(value, "$indent    ")
				}
				println("$indent\u001b[96m}")
			}
			is JsonArray -> {
				println("\u001b[96m[")
				for(value in json) {
					print("$indent  ")
					dump(value, "$indent    ")
				}
				println("$indent\u001b[96m]")
			}
			is JsonPrimitive -> {
				println("\u001b[0m$json")
			}
		}
		dump(json, "")
	}
}

fun HttpBodies.form(block: FormScope.() -> Unit): HttpBody = object : WriterHttpBody {
	override fun write(out: Writer) {
		out.write(FormScope().apply(block).build())
	}
	
	override val contentType: String get() = "application/x-www-form-urlencoded"
	
	override fun writeDebug(out: Writer) {
		val form = FormScope().apply(block)
		out.write(form.build())
		println("\u001b[90m(html form)")
		for((key, value) in form.content) {
			println("\u001b[91m$key\u001b[0m=\u001b[96m$value\u001b[0m")
		}
	}
	
	override val debugAvailable: Boolean get() = true
}

@StructureBuilder
class FormScope {
	val content = mutableMapOf<String, String>()
	
	infix fun String.set(value: String) {
		content[this] = value
	}
	
	fun build(): String = queryUrlParamsToString(content)
}