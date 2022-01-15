package com.lhwdev.selfTestMacro.database

import android.content.SharedPreferences


class PreferenceHolder(val pref: SharedPreferences) {
	interface Property {
		fun onUpdated()
	}
	
	
	private val properties = mutableMapOf<String, Property>()
	
	private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if(key != null) properties[key]?.onUpdated()
	}
	
	init {
		pref.registerOnSharedPreferenceChangeListener(listener)
	}
	
	
	// supposed to be property registered permanently
	fun property(key: String, value: Property) {
		properties[key] = value
	}
}
