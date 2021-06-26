package com.lhwdev.selfTestMacro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.util.WeakHashMap


class PreferenceState(private val pref: SharedPreferences) {
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
	
	val db: DatabaseManager = DatabaseManager(pref)
	
	var headUser by pref.preferenceInt(key = "headUser", defaultValue = 0)
	
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


fun Context.prefMain(): SharedPreferences =
	getSharedPreferences("main", AppCompatActivity.MODE_PRIVATE)


fun Context.createIntent(): PendingIntent = PendingIntent.getBroadcast(
	this, AlarmReceiver.REQUEST_CODE, Intent(this, AlarmReceiver::class.java),
	PendingIntent.FLAG_UPDATE_CURRENT
)
