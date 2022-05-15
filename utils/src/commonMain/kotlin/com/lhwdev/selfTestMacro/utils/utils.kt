@file:Suppress("SpellCheckingInspection")

package com.lhwdev.selfTestMacro.utils


@PublishedApi
internal val sNone = Any()


// not thread-safe; if you want, use Collections.synchronizedList
abstract class LazyListBase<E>(final override val size: Int) : AbstractList<E>() {
	private val cache = MutableList<Any?>(size) { sNone }
	
	protected abstract fun createAt(index: Int): E
	
	override fun get(index: Int): E {
		val element = cache[index]
		val result = if(element === sNone) {
			val new = createAt(index)
			cache[index] = new
			new
		} else element
		
		@Suppress("UNCHECKED_CAST")
		return result as E
	}
}

fun <T> Iterable<T>.asList(): List<T> = when(this) {
	is List<T> -> this
	else -> toList()
}

inline fun <T, R> List<T>.lazyMap(
	crossinline block: (T) -> R
): List<R> = object : LazyListBase<R>(size) {
	private val list = this@lazyMap
	override fun createAt(index: Int): R = block(list[index])
}


fun String.splitTwo(by: Char): Pair<String, String> {
	val index = indexOf(by)
	check(index != -1) { "'$by' is not found in '$this'" }
	return take(index) to drop(index + 1)
}
