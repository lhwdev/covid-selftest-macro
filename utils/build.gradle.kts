plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
}



kotlin {
	jvm()
	
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")
				
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.0")
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
				
				implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
			}
			
		}
	}
}
