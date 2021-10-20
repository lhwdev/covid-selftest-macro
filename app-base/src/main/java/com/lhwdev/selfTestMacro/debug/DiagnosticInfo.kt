package com.lhwdev.selfTestMacro.debug

import kotlin.reflect.KClass


interface DiagnosticObject {
	fun getDiagnosticInformation(): DiagnosticItem
}


fun DiagnosticItem.asObject(): DiagnosticObject = object : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = this@asObject
}

sealed interface DiagnosticItem {
	val name: String
}

interface DiagnosticItemGroup : DiagnosticItem {
	val children: List<DiagnosticItem>
}

interface DiagnosticElement : DiagnosticItem {
	val data: Any?
	val type: KClass<*>?
}

class SimpleDiagnosticElement(override val name: String, override val data: Any?, override val type: KClass<*>) :
	DiagnosticElement


inline fun <reified T> diagnosticElement(name: String, data: T): SimpleDiagnosticElement =
	SimpleDiagnosticElement(name, data, T::class)

inline fun diagnosticElements(block: DiagnosticItemGroupBuilder.() -> Unit): List<DiagnosticElement> =
	DiagnosticItemGroupBuilder().apply(block).build()

class DiagnosticItemGroupBuilder {
	@PublishedApi
	internal val list = mutableListOf<DiagnosticElement>()
	
	inline infix fun <reified T> String.set(data: T) {
		list += diagnosticElement(this, data)
	}
	
	fun build(): List<DiagnosticElement> = list
}

object EmptyDiagnosticGroup : DiagnosticItemGroup {
	override val name: String get() = "<empty>"
	override val children: List<DiagnosticItem> = emptyList()
}


fun DiagnosticItem.dump(oneLine: Boolean): String = buildString {
	fun dump(item: DiagnosticItem, depth: Int): Any = when(item) {
		is DiagnosticElement -> {
			append("  ".repeat(depth - 1))
			if(depth != 0) append("|-")
			append(" ")
			append(item.name)
			append(": ")
			append(item.data)
		}
		is DiagnosticItemGroup -> {
			append(item.name)
			for(child in item.children) {
				append('\n')
				dump(child, depth + 1)
			}
		}
	}
	
	fun dumpOneLine(item: DiagnosticItem): Any = when(item) {
		is DiagnosticElement -> {
			append(item.name)
			append(": ")
			append(item.data)
		}
		is DiagnosticItemGroup -> {
			append(item.name)
			append(": [ ")
			for((index, child) in item.children.withIndex()) {
				if(index != 0) append(", ")
				dumpOneLine(child)
			}
			append(" ]")
		}
	}
	
	if(oneLine) dumpOneLine(this@dump)
	else dump(this@dump, depth = 0)
}

