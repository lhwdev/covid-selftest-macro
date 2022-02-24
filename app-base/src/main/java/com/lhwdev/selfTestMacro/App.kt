package com.lhwdev.selfTestMacro

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import com.lhwdev.selfTestMacro.database.PreferenceItemState
import com.lhwdev.selfTestMacro.database.preferenceHolderOf
import com.lhwdev.selfTestMacro.database.preferenceInt
import com.lhwdev.selfTestMacro.models.Version
import java.io.File


object AppInitializationInfo { // TODO: is this time to introduce DI?
	var versionCode: Int = 0
	lateinit var versionName: String
	lateinit var github: GithubDataModel
	lateinit var flavor: String
	lateinit var debugLogDirectory: File
	lateinit var mainActivity: Class<*>
	var appIconForeground: Int = 0
	var appIcon: Int = 0
	var debug: Boolean = false
	
	var initialized: Boolean = false
}


object App {
	init {
		if(!AppInitializationInfo.initialized) error("not initialized")
	}
	
	val version: Version = Version(AppInitializationInfo.versionName)
	val versionCode: Int = AppInitializationInfo.versionCode
	
	val flavor: String = AppInitializationInfo.flavor
	val debug: Boolean = AppInitializationInfo.debug
	
	val debugLogDirectory: File = AppInitializationInfo.debugLogDirectory
	
	fun mainActivityIntent(context: Context): Intent = Intent(context, AppInitializationInfo.mainActivity)
	
	@DrawableRes
	val appIconForeground: Int = AppInitializationInfo.appIconForeground
	
	@DrawableRes
	val appIcon: Int = AppInitializationInfo.appIcon
	
	val github: GithubDataModel = AppInitializationInfo.github
	
	fun lastVersion(key: String, context: Context): LastVersion = LastVersion(
		state = context.preferenceHolderOf("App.lastVersions").preferenceInt(key, -1)
	)
}

class LastVersion(private val state: PreferenceItemState<Int>) {
	fun last(current: Int = App.versionCode): Int {
		val last = state.value
		if(last == -1) {
			state.value = current
			return current
		}
		if(last < current) {
			state.value = current
			return last
		}
		
		return current
	}
	
	fun isOld(current: Int): Boolean = last(current) != current
}


val App.debuggingWithIde: Boolean
	get() = flavor == "dev"
