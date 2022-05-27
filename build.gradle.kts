plugins {
	id("com.android.application") version "7.0.1" apply false
	
	val kotlinVersion = libs.versions.kotlin
	kotlin("multiplatform") version kotlinVersion apply false
	kotlin("jvm") version kotlinVersion apply false
	kotlin("android") version kotlinVersion apply false
	kotlin("plugin.serialization") version kotlinVersion apply false
	
	id("org.jetbrains.compose") version libs.versions.compose apply false
}


buildscript {
	repositories {
		google()
		mavenCentral()
		maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}

allprojects {
	repositories {
		google()
		mavenCentral()
		maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}

tasks.register<Delete>("clean") {
	delete(rootProject.buildDir)
}
