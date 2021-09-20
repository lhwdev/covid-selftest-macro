package com.lhwdev.selfTestMacro.database

import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
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
	private val cache = mutableStateOf<T?>(null)
	
	
	override var value: T
		get() {
			return if(handle.clean) {
				@Suppress("UNCHECKED_CAST")
				cache.value as T
			} else {
				val newValue = read(pref)
				cache.value = newValue
				handle.clean = true
				newValue
			}
		}
		set(value) {
			if(cache.value == value) return
			
			cache.value = value
			val current = currentTransaction
			if(current == null) {
				write(pref, value)
				handle.clean = true // forcibly; write() would make handle.clean false
			} else {
				current[this] = {
					write(pref, value)
					handle.clean = true // forcibly; write() would make handle.clean false
				}
				handle.clean = true
			}
		}
	
	override fun component1(): T = value
	override fun component2(): (T) -> Unit = { value = it }
	
	override val policy: SnapshotMutationPolicy<T>
		@Suppress("UNCHECKED_CAST")
		get() = structuralEqualityPolicy()
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
