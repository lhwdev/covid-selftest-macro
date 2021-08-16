rootProject.name = "SelfTest-Macro"
include(":app", ":transkey", ":api-base", ":api", ":test")


pluginManagement {
	plugins {
		id("com.android.application") version "4.2.2"
		
		val kotlinVersion = "1.5.21" // compose + serialization
		kotlin("multiplatform") version kotlinVersion
		kotlin("jvm") version kotlinVersion
		kotlin("android") version kotlinVersion
		kotlin("plugin.serialization") version kotlinVersion
	}
	
	repositories {
		gradlePluginPortal()
		google()
	}
	
	resolutionStrategy.eachPlugin {
		when(requested.id.id) {
			"com.android.application", "com.android.library" ->
				useModule("com.android.tools.build:gradle:4.2.2")
			
			else -> Unit
		}
	}
}
