package com.lhwdev.selfTestMacro

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import com.lhwdev.selfTestMacro.models.Version
import java.io.File


object AppInitializationInfo {
	var versionCode: Int = 0
	lateinit var versionName: String
	lateinit var github: GithubDataModel
	lateinit var flavor: String
	lateinit var debugLogDirectory: File
	lateinit var mainActivity: Class<*>
	var appIconForeground: Int = 0
	var appIcon: Int = 0
	var debug: Boolean = false
}


object App {
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
}


val App.debuggingWithIde: Boolean
	get() = flavor == "dev"
