package com.lhwdev.selfTestMacro

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.core.content.edit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json


inline fun <T> PreferenceHolder.preferenceState(
	key: String,
	crossinline read: (SharedPreferences) -> T,
	crossinline write: (SharedPreferences, T) -> Unit
): MutableState<T> = object : SnapshotMutableState<T> {
	private val handle = property(key)
	private var cache: T? by mutableStateOf(null)
	
	override var value: T
		get() {
			return if(handle.clean) {
				@Suppress("UNCHECKED_CAST")
				cache as T
			} else {
				val newValue = read(pref)
				cache = newValue
				handle.clean = true
				newValue
			}
		}
		set(value) {
			cache = value
			write(pref, value)
			handle.clean = true // forcibly; write() would make handle.clean false
		}
	
	override fun component1(): T = value
	override fun component2(): (T) -> Unit = { value = it }
	override val policy: SnapshotMutationPolicy<T> get() = structuralEqualityPolicy()
}


@PublishedApi
internal val sNone = Any()

// not thread-safe; if you want, use Collections.synchronizedList
abstract class LazyListBase<E>(final override val size: Int) : AbstractList<E>() {
	val cache = MutableList<Any?>(size) { sNone }
	
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

fun PreferenceHolder.preferenceInt(
	key: String, defaultValue: Int
): MutableState<Int> = preferenceState(
	key = key,
	read = { pref -> pref.getInt(key, defaultValue) },
	write = { pref, value -> pref.edit { putInt(key, value) } }
)

fun PreferenceHolder.preferenceBoolean(
	key: String, defaultValue: Boolean
): MutableState<Boolean> = preferenceState(
	key = key,
	read = { pref -> pref.getBoolean(key, defaultValue) },
	write = { pref, value -> pref.edit { putBoolean(key, value) } }
)

fun PreferenceHolder.preferenceString(
	key: String, defaultValue: String? = null
): MutableState<String?> = preferenceState(
	key = key,
	read = { pref -> pref.getString(key, defaultValue) },
	write = { pref, value -> pref.edit { putString(key, value) } }
)

fun PreferenceHolder.preferenceStringSet(
	key: String, defaultValue: Set<String>
): MutableState<Set<String>> = preferenceState(
	key = key,
	read = { pref -> pref.getStringSet(key, defaultValue)!! },
	write = { pref, value -> pref.edit { putStringSet(key, value) } }
)

@OptIn(ExperimentalSerializationApi::class)
fun <T> PreferenceHolder.preferenceSerialized(
	key: String,
	serializer: KSerializer<T>,
	defaultValue: T,
	formatter: StringFormat = Json
): MutableState<T> = preferenceState(
	key = key,
	read = { pref ->
		val string = pref.getString(key, null)
		if(string == null) {
			defaultValue
		} else {
			formatter.decodeFromString(serializer, string)
		}
	},
	write = { pref, value ->
		pref.edit {
			putString(key, formatter.encodeToString(serializer, value))
		}
	}
)
