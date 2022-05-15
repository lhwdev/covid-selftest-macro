package com.lhwdev.selfTestMacro.database

import android.content.SharedPreferences
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.withCurrent
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.core.content.edit
import com.lhwdev.selfTestMacro.utils.SynchronizedMutableState
import com.lhwdev.selfTestMacro.utils.SynchronizedMutableStateImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json


interface PreferenceItemState<T> : SynchronizedMutableState<T>, SnapshotMutableState<T>, PreferenceHolder.Property


abstract class PreferenceItemStateImpl<T>(protected val holder: PreferenceHolder, key: String) :
	SynchronizedMutableStateImpl<T>(structuralEqualityPolicy()), PreferenceItemState<T> {
	override fun onPropertyUpdated() {
		next.withCurrent { it.emptyCache() }
	}
}

inline fun <T> PreferenceHolder.preferenceState(
	key: String,
	crossinline read: (SharedPreferences) -> T,
	crossinline write: (SharedPreferences, T) -> Unit
): PreferenceItemState<T> = property<PreferenceItemState<T>>(key) {
	object : PreferenceItemStateImpl<T>(holder = this, key = key) {
		override fun read(): T = read(holder.pref)
		
		override fun write(value: T) {
			write(holder.pref, value)
		}
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
