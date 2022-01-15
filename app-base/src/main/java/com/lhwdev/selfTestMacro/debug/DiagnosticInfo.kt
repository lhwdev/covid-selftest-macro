package com.lhwdev.selfTestMacro.debug

import kotlin.reflect.KClass


interface DiagnosticObject {
	fun getDiagnosticInformation(): DiagnosticItem
}


fun DiagnosticItem.asObject(): DiagnosticObject = object : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = this@asObject
}

sealed interface DiagnosticItem : DiagnosticObject {
	val name: String
	val localizedName: String?
	
	override fun getDiagnosticInformation(): DiagnosticItem = this
}

interface DiagnosticItemGroup : DiagnosticItem {
	val children: List<DiagnosticItem>
}

interface DiagnosticElement<T : Any> : DiagnosticItem {
	val data: T?
	val type: KClass<T>?
	
	val localizeData: ((T) -> String)? get() = null
}

class SimpleDiagnosticElement<T : Any>(
	override val name: String,
	override val localizedName: String?,
	override val data: T?,
	override val type: KClass<T>,
	override val localizeData: ((T) -> String)? = null
) : DiagnosticElement<T>

class SimpleDiagnosticItemGroup(
	override val name: String,
	override val localizedName: String?,
	override val children: List<DiagnosticItem>
) : DiagnosticItemGroup

inline fun <reified T : Any> diagnosticElement(
	name: String,
	localizedName: String?,
	data: T
): SimpleDiagnosticElement<T> =
	SimpleDiagnosticElement(name, localizedName, data, T::class)

inline fun diagnosticElements(block: DiagnosticItemGroupBuilder.() -> Unit): List<DiagnosticItem> =
	DiagnosticItemGroupBuilder().apply(block).build()

inline fun diagnosticGroup(
	name: String,
	localizedName: String?,
	block: DiagnosticItemGroupBuilder.() -> Unit
): DiagnosticItemGroup = SimpleDiagnosticItemGroup(
	name = name,
	localizedName = localizedName,
	children = diagnosticElements(block)
)

class BuildingDiagnosticElement<T : Any>(
	override val name: String,
	override var localizedName: String?,
	override val data: T?,
	override val type: KClass<T>,
	override var localizeData: ((T) -> String)? = null
) : DiagnosticElement<T>

class DiagnosticItemGroupBuilder {
	private val list = mutableListOf<DiagnosticItem>()
	
	inline infix fun <reified T : Any> String.set(data: T?): DiagnosticElement<T> =
		set(name = this, data = data, type = T::class)
	
	infix fun <T : Any> DiagnosticElement<T>.localized(name: String): DiagnosticElement<T> =
		also { (it as BuildingDiagnosticElement<T>).localizedName = name }
	
	
	infix fun <T : Any> DiagnosticElement<T>.localizeData(block: (T) -> String): DiagnosticElement<T> =
		also { (it as BuildingDiagnosticElement<T>).localizeData = block }
	
	fun <T : Any> set(name: String, data: T?, type: KClass<T>): DiagnosticElement<T> {
		val element = BuildingDiagnosticElement(name = name, localizedName = null, data = data, type = type)
		list += element
		return element
	}
	
	infix fun String.set(element: DiagnosticObject) {
		list += element.getDiagnosticInformation()
	}
	
	fun build(): List<DiagnosticItem> = list
}

object EmptyDiagnosticGroup : DiagnosticItemGroup {
	override val name: String get() = "<empty>"
	override val localizedName: String? get() = null
	override val children: List<DiagnosticItem> = emptyList()
}


fun DiagnosticElement<*>.localizedData(): String {
	val localize = localizeData
	if(localize != null) {
		@Suppress("UNCHECKED_CAST")
		return (localize as (Any?) -> String).invoke(data)
	}
	
	return when(val data = data) {
		null -> "(없음)"
		is Boolean -> if(data) "예" else "아니오"
		else -> "$data"
	}
}

fun DiagnosticObject.dumpDebug(oneLine: Boolean): String = getDiagnosticInformation().dumpDebug(oneLine = oneLine)

fun DiagnosticItem.dumpDebug(oneLine: Boolean): String = buildString {
	fun dump(item: DiagnosticItem, depth: Int): Any = when(item) {
		is DiagnosticElement<*> -> {
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
		is DiagnosticElement<*> -> {
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
	
	if(oneLine) dumpOneLine(this@dumpDebug)
	else dump(this@dumpDebug, depth = 0)
}

fun DiagnosticItem.dumpLocalized(oneLine: Boolean): String = buildString {
	fun dump(item: DiagnosticItem, depth: Int) {
		when(item) {
			is DiagnosticElement<*> -> if(item.localizedName != null) {
				append("  ".repeat(depth - 1))
				if(depth != 0) append("|-")
				append(" ")
				append(item.localizedName)
				append(": ")
				append(item.localizedData())
			}
			is DiagnosticItemGroup -> {
				append(item.name)
				for(child in item.children) {
					append('\n')
					dump(child, depth + 1)
				}
			}
		}
	}
	
	fun dumpOneLine(item: DiagnosticItem) {
		when(item) {
			is DiagnosticElement<*> -> if(item.localizedName != null) {
				append(item.localizedName)
				append(": ")
				append(item.localizedData())
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
	}
	
	if(oneLine) dumpOneLine(this@dumpLocalized)
	else dump(this@dumpLocalized, depth = 0)
}

