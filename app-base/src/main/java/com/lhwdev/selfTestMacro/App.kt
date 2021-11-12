package com.lhwdev.selfTestMacro

import androidx.annotation.DrawableRes
import com.lhwdev.github.repo.Repository
import com.lhwdev.github.repo.branch
import com.lhwdev.github.repo.defaultBranch
import com.lhwdev.selfTestMacro.models.Version
import java.io.File


object AppInitializationInfo {
	var versionCode: Int = 0
	lateinit var versionName: String
	lateinit var githubRepo: Repository
	lateinit var flavor: String
	lateinit var debugLogDirectory: File
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
	
	@DrawableRes
	val appIconForeground: Int = AppInitializationInfo.appIconForeground
	
	@DrawableRes
	val appIcon: Int = AppInitializationInfo.appIcon
	
	val githubRepo: Repository = AppInitializationInfo.githubRepo
	val masterBranch = githubRepo.defaultBranch()
	val metaBranch = githubRepo.branch("app-meta")
}


val App.debuggingWithIde: Boolean
	get() = flavor == "dev"
