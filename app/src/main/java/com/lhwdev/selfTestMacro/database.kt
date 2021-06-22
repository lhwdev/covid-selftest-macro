package com.lhwdev.selfTestMacro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import java.util.WeakHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class PreferenceState(val pref: SharedPreferences) {
	init {
		// version migration
		when(pref.getInt("lastVersion", -1)) {
			in -1..999 -> pref.edit { clear() }
			in 1000..1006 -> pref.edit { clear() }
			BuildConfig.VERSION_CODE -> Unit // latest
		}
		
		pref.edit { putInt("lastVersion", BuildConfig.VERSION_CODE) }
	}
	
	var isDebugEnabled by pref.preferenceBoolean("isDebugEnabled", false)
	
	var firstState by pref.preferenceInt("first", 0)
	var isSchedulingEnabled by pref.preferenceBoolean("isSchedulingEnabled", false)
	var hour by pref.preferenceInt("hour", -1)
	var min by pref.preferenceInt("min", 0)
	
	var testGroups by pref.preferenceSerialized(
		key = "testGroups",
		serializer = DbTestGroups.serializer(),
		defaultValue = DbTestGroups()
	)
	var userGroups by pref.preferenceSerialized(
		key = "testGroups",
		serializer = DbUserGroups.serializer(),
		defaultValue = DbUserGroups()
	)
	var users by pref.preferenceSerialized(
		key = "users",
		serializer = DbUsers.serializer(),
		defaultValue = DbUsers()
	)
	
	var shownNotices: Set<String>
		get() = pref.getStringSet("shownNotices", setOf())!!
		set(value) = pref.edit {
			putStringSet("shownNotices", value)
		}
	var doNotShowAgainNotices: Set<String>
		get() = pref.getStringSet("shownNotices", setOf())!!
		set(value) = pref.edit {
			putStringSet("shownNotices", value)
		}
}


// I knew that global things are bad in android(i waz lazy), but didn't know would be by far worst.
// Only one line below caused TWO bugs; I WON'T do like this in the future
//
// but, some decent ways to do this?
// 1. always passing it through argument: so complicated
// 2. keeping global with the initialization of Application; here [MainApplication]
// 3. passing via argument, but through extension receiver
// 4. like Local? : unsafe(though safer in Jetpack Compose)
// 5. ThreadLocal: what else from the original one

// ok, maybe decent way; pooling from cache
private val preferenceStateMap = WeakHashMap<Context, PreferenceState>()

val Context.preferenceState: PreferenceState
	get() = preferenceStateMap.getOrPut(applicationContext) {
		PreferenceState(prefMain())
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
			if(value == null) remove(key)
			else putString(key, formatter.encodeToString(serializer, value))
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

fun Context.prefMain(): SharedPreferences =
	getSharedPreferences("main", AppCompatActivity.MODE_PRIVATE)


fun Context.createIntent(): PendingIntent = PendingIntent.getBroadcast(
	this, AlarmReceiver.REQUEST_CODE, Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT
)
