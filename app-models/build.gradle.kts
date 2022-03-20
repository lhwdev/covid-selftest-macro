plugins {
	id("com.android.library")
	kotlin("android")
	kotlin("plugin.serialization")
	
	id("common-plugin")
}


android {
	compileSdk = 31
	
	defaultConfig {
		minSdk = 21
		targetSdk = 30
	}
}


dependencies {
	implementation(projects.utils)
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(libs.immutableCollections)
}
