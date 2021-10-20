package com.lhwdev.selfTestMacro.debug

import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


interface DiagnosticObject {
	fun getDiagnosticInformation(): DiagnosticItem
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

private class PropertyDiagnosticElement<T>(val property: KProperty0<T>) : DiagnosticElement {
	override val name: String
		get() = property.name
	override val data: Any?
		get() = property.get()
	override val type: KClass<*>?
		get() = null
}


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
