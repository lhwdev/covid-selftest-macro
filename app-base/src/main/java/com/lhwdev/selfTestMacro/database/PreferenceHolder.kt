package com.lhwdev.selfTestMacro.database

import android.content.Context
import android.content.SharedPreferences


// Reference to SharedPreference is safe for static
// https://stackoverflow.com/questions/22544466/is-it-safe-to-keep-a-static-reference-to-a-sharedpreferences-and-its-editor
@PublishedApi
internal val preferenceHolders = mutableMapOf<String, PreferenceHolder>()


fun Context.preferenceHolderOf(key: String): PreferenceHolder = preferenceHolderOf<PreferenceHolder>(key) { pref ->
	DefaultPreferenceHolder(pref = pref)
}

inline fun <reified T : PreferenceHolder> Context.preferenceHolderOf(
	key: String, create: (SharedPreferences) -> T
): T {
	val last = preferenceHolders[key]
	if(last is T) return last
	return create(getSharedPreferences(key, Context.MODE_PRIVATE)).also { preferenceHolders[key] = it }
}


class DefaultPreferenceHolder(pref: SharedPreferences) : PreferenceHolder(pref)

abstract class PreferenceHolder constructor(val pref: SharedPreferences) {
	interface Property {
		fun onUpdated()
	}
	
	
	@PublishedApi
	internal val properties = mutableMapOf<String, Property>()
	
	private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if(key != null) properties[key]?.onUpdated()
	}
	
	init {
		pref.registerOnSharedPreferenceChangeListener(listener)
	}
	
	inline fun <reified T : Property> property(key: String, create: () -> T): T {
		val last = properties[key]
		if(last is T) {
			return last
		}
		
		return create().also {
			properties[key] = it
		}
	}
}
