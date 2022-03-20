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
	android("android")
	jvm("desktop")
	
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(projects.utils)
				
				implementation(libs.coroutinesAndroid)
				
				implementation(libs.serializationCore)
				implementation(libs.serializationJson)
			}
		}
		
		register("jvmMain")
		val jvmMain by getting {
			dependsOn(commonMain)
		}
		
		val androidMain by getting {
			dependsOn(jvmMain)
		}
		
		val desktopMain by getting {
			dependsOn(jvmMain)
		}
	}
}
