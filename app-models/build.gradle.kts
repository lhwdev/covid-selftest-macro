plugins {
	id("com.android.library")
	kotlin("android")
	kotlin("plugin.serialization")
}


android {
	compileSdk = 31
	
	defaultConfig {
		minSdk = 21
		targetSdk = 30
	}
}


dependencies {
	implementation(project(":utils"))
	
	implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")
	
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
	
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
}
