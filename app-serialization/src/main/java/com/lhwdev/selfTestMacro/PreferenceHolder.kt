package com.lhwdev.selfTestMacro

import android.content.SharedPreferences


class PreferenceHolder(val pref: SharedPreferences) {
	private val propertyClean = mutableMapOf<String, Boolean>()
	
	private val listener = pref.registerOnSharedPreferenceChangeListener { _, key ->
		if(key != null) propertyClean[key] = false
	}
	
	
	inner class PropertyHandle(private val key: String) {
		/**
		 * false by default
		 */
		var clean: Boolean
			get() = propertyClean[key] == true
			set(value) {
				propertyClean[key] = value
			}
	}
	
	
	// supposed to be property registered permanently
	fun property(key: String): PropertyHandle = PropertyHandle(key)
}
