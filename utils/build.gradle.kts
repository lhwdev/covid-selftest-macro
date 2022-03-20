plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
	
	id("common-plugin")
}



kotlin {
	jvm()
	
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(libs.coroutinesAndroid)
				
				implementation(libs.serializationCore)
				implementation(libs.serializationJson)
				
				implementation(libs.immutableCollections)
			}
		}
	}
}
