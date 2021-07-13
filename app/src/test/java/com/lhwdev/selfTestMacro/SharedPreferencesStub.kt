package com.lhwdev.selfTestMacro

import android.content.SharedPreferences


class SharedPreferenceBuilder {
	private val data = mutableMapOf<String, Any?>()
	infix fun String.to(value: Any?) {
		data[this] = value
	}
	
	fun build(): SharedPreferencesStub = SharedPreferencesStub(data)
}


inline fun buildSharedPreference(block: SharedPreferenceBuilder.() -> Unit): SharedPreferencesStub =
	SharedPreferenceBuilder().apply(block).build()


class SharedPreferencesStub(data: Map<String, Any?>) : SharedPreferences {
	val data = data.toMutableMap()
	private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
	
	private inline fun <reified T> getOr(key: String, defValue: T): T =
		data.getOrElse(key) { defValue } as? T ?: defValue
	
	
	override fun getAll(): MutableMap<String, *> = data
	
	override fun getString(key: String, defValue: String?): String? = getOr(key, defValue)
	
	@Suppress("UNCHECKED_CAST")
	override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
		getOr(key, defValues)
	
	override fun getInt(key: String, defValue: Int): Int = getOr(key, defValue)
	
	override fun getLong(key: String, defValue: Long): Long = getOr(key, defValue)
	
	override fun getFloat(key: String, defValue: Float): Float = getOr(key, defValue)
	
	override fun getBoolean(key: String, defValue: Boolean): Boolean = getOr(key, defValue)
	
	override fun contains(key: String): Boolean = key in data
	
	override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
		private val mutation = mutableMapOf<String, Any?>()
		
		override fun putString(key: String, value: String?): SharedPreferences.Editor {
			mutation[key] = value
			return this
		}
		
		override fun putStringSet(
			key: String,
			values: MutableSet<String>?
		): SharedPreferences.Editor {
			mutation[key] = values
			return this
		}
		
		override fun putInt(key: String, value: Int): SharedPreferences.Editor {
			mutation[key] = value
			return this
		}
		
		override fun putLong(key: String, value: Long): SharedPreferences.Editor {
			mutation[key] = value
			return this
		}
		
		override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
			mutation[key] = value
			return this
		}
		
		override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
			mutation[key] = value
			return this
		}
		
		override fun remove(key: String): SharedPreferences.Editor {
			mutation.remove(key)
			return this
		}
		
		override fun clear(): SharedPreferences.Editor {
			mutation.clear()
			return this
		}
		
		override fun commit(): Boolean {
			data += mutation
			for(key in mutation.keys) {
				listeners.forEach { it.onSharedPreferenceChanged(this@SharedPreferencesStub, key) }
			}
			return true
		}
		
		override fun apply() {
			commit()
		}
	}
	
	override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		listeners += listener
	}
	
	override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		listeners -= listener
	}
}
