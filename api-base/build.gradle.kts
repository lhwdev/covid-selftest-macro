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

commonConfig {
	kotlin {
		val jvm = intermediate("jvm") { dependsOn(common) }
		
		android("android") { dependsOn(jvm) }
		jvm("desktop") { dependsOn(jvm) }
		
		dependencies {
			implementation(projects.utils)
			
			implementation(libs.coroutinesCore)
			
			implementation(libs.serializationCore)
			implementation(libs.serializationJson)
		}
	}
}
