package com.lhwdev.selfTestMacro

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


@PublishedApi
internal val sNone = Any()

// not thread-safe; if you want, use Collections.synchronizedList
abstract class LazyListBase<E>(final override val size: Int) : AbstractList<E>() {
	val cache = MutableList<Any?>(size) { null }
	
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

inline fun <T, R> List<T>.lazyMap(
	crossinline block: (T) -> R
): List<R> = object : LazyListBase<R>(size) {
	private val list = this@lazyMap
	override fun createAt(index: Int): R = block(list[index])
}

fun SharedPreferences.preferenceInt(key: String, defaultValue: Int) =
	object : ReadWriteProperty<Any?, Int> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
			edit { putInt(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
			getInt(key, defaultValue)
	}

fun SharedPreferences.preferenceBoolean(key: String, defaultValue: Boolean) =
	object : ReadWriteProperty<Any?, Boolean> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
			edit { putBoolean(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
			getBoolean(key, defaultValue)
	}

fun SharedPreferences.preferenceString(key: String, defaultValue: String? = null) =
	object : ReadWriteProperty<Any?, String?> {
		override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
			edit { putString(key, value) }
		}
		
		override fun getValue(thisRef: Any?, property: KProperty<*>): String? =
			getString(key, defaultValue)
	}

@OptIn(ExperimentalSerializationApi::class)
fun <T> SharedPreferences.preferenceSerialized(
	key: String,
	serializer: KSerializer<T>,
	defaultValue: T,
	formatter: StringFormat = Json
) = object : ReadWriteProperty<Any?, T> {
	var updated = false
	var cache: T = defaultValue
	
	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		cache = value
		updated = true
		edit {
			putString(key, formatter.encodeToString(serializer, value))
		}
	}
	
	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		if(!updated) {
			val string = getString(key, null)
			cache =
				if(string == null) defaultValue else formatter.decodeFromString(serializer, string)
		}
		return cache
	}
}
