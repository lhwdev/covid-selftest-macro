package com.lhwdev.selfTestMacro.database

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.lhwdev.github.repo.Repository
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.LastVersion
import kotlinx.serialization.builtins.nullable
import java.util.WeakHashMap


class PreferenceState(pref: SharedPreferences) : PreferenceHolder(pref) {
	init {
		// version migration
		when(LastVersion(preferenceInt("lastVersion", -1)).last()) {
			App.versionCode -> Unit // latest
			in -1..999 -> pref.edit { clear() }
			in 1000..3000 -> pref.edit { clear() }
			else -> Unit
		}
	}
	
	
	// val defaultUpdateChannel = "stable"
	// var updateChannel by pref.preferenceString("updateChannel", defaultUpdateChannel)
	
	var isDebugEnabled by preferenceBoolean("isDebugEnabled", false)
	var isDebugCheckEnabled by preferenceBoolean("isDebugCheckEnabled", false)
	var includeLogcatInLog by preferenceBoolean("includeLogcatInLog", false)
	var virtualServer by preferenceSerialized("isVirtualServer", Repository.serializer().nullable, null)
	var isDebugFetchEnabled by preferenceBoolean("isDebugFetchEnabled", false)
	var isNavigationDebugEnabled by preferenceBoolean("isDebugAnimateListAsComposableEnabled", false)
	
	var isFirstTime by preferenceBoolean("first", true)
	
	val db: AppDatabase = AppDatabase(this)
	
	var headUser by preferenceInt(key = "headUser", defaultValue = 0)
	
	var shownNotices: Set<String> by preferenceStringSet("shownNotices", emptySet())
	var doNotShowAgainNotices: Set<String> by preferenceStringSet("doNotShowAgainNotices", emptySet())
}


private val preferenceStateMap = WeakHashMap<Context, PreferenceState>()

val Context.preferenceState: PreferenceState
	get() = preferenceHolderOf("main") { pref -> PreferenceState(pref) }
