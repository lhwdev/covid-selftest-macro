import com.lhwdev.build.*

plugins {
	id("com.android.library")
	kotlin("multiplatform")
	kotlin("plugin.serialization")
	
	id("common-plugin")
}

android {
	defaultConfig {
		minSdk = 19
		compileSdk = 31
	}
}

kotlin {
	val jvm = setupJvm("jvm") {
		dependsOnCommon()
	}
	setupAndroid(project, "android") {
		dependsOn(jvm)
	}
	setupJvm("desktop") {
		dependsOn(jvm)
	}
	
	
	dependencies {
		implementation(projects.utils)
		
		implementation(libs.coroutinesCore)
		
		implementation(libs.serializationCore)
		implementation(libs.serializationJson)
	}
}
