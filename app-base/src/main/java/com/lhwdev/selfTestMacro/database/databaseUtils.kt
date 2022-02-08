package com.lhwdev.selfTestMacro.database

import android.content.SharedPreferences
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.core.content.edit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json


private val sEmpty = Any()


interface PreferenceItemState<T> : SnapshotMutableState<T> {
	fun forceWrite()
}


abstract class PreferenceItemStateImpl<T>(protected val holder: PreferenceHolder, key: String) :
	PreferenceItemState<T>, PreferenceHolder.Property {
	private val cache = mutableStateOf<Any?>(sEmpty)
	
	
	protected abstract fun read(): T
	protected abstract fun write(value: T)
	
	
	override fun forceWrite() {
		@Suppress("UNCHECKED_CAST")
		write(cache.value as T)
	}
	
	
	override var value: T
		get() {
			val previous = cache.value
			
			return if(previous !== sEmpty) {
				@Suppress("UNCHECKED_CAST")
				previous as T
			} else {
				val newValue = read()
				cache.value = newValue
				newValue
			}
		}
		set(value) {
			if(cache.value == value) return
			
			val current = currentDbTransaction
			if(current == null) {
				write(value)
				cache.value = value
			} else {
				current.operations[this] = {
					write(value)
					cache.value = value
				}
				cache.value = value
			}
		}
	
	override fun onUpdated() {
		cache.value = sEmpty
	}
	
	override fun component1(): T = value
	override fun component2(): (T) -> Unit = { value = it }
	
	override val policy: SnapshotMutationPolicy<T>
		@Suppress("UNCHECKED_CAST")
		get() = structuralEqualityPolicy()
	
	
	init {
		@Suppress("LeakingThis") // property() do nothing than putting this instance in map
		holder.property(key, this)
	}
}

inline fun <T> PreferenceHolder.preferenceState(
	key: String,
	crossinline read: (SharedPreferences) -> T,
	crossinline write: (SharedPreferences, T) -> Unit
): PreferenceItemState<T> = object : PreferenceItemStateImpl<T>(holder = this, key = key) {
	override fun read(): T = read(holder.pref)
	
	override fun write(value: T) {
		write(holder.pref, value)
	}
}


fun PreferenceHolder.preferenceInt(
	key: String, defaultValue: Int
): PreferenceItemState<Int> = preferenceState(
	key = key,
	read = { pref -> pref.getInt(key, defaultValue) },
	write = { pref, value -> pref.edit { putInt(key, value) } }
)

fun PreferenceHolder.preferenceLong(
	key: String, defaultValue: Long
): PreferenceItemState<Long> = preferenceState(
	key = key,
	read = { pref -> pref.getLong(key, defaultValue) },
	write = { pref, value -> pref.edit { putLong(key, value) } }
)

fun PreferenceHolder.preferenceBoolean(
	key: String, defaultValue: Boolean
): PreferenceItemState<Boolean> = preferenceState(
	key = key,
	read = { pref -> pref.getBoolean(key, defaultValue) },
	write = { pref, value -> pref.edit { putBoolean(key, value) } }
)

fun PreferenceHolder.preferenceString(
	key: String, defaultValue: String? = null
): PreferenceItemState<String?> = preferenceState(
	key = key,
	read = { pref -> pref.getString(key, defaultValue) },
	write = { pref, value -> pref.edit { putString(key, value) } }
)

fun PreferenceHolder.preferenceStringSet(
	key: String, defaultValue: Set<String>
): PreferenceItemState<Set<String>> = preferenceState(
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
): PreferenceItemState<T> = preferenceState(
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
