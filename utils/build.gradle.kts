import com.lhwdev.build.*
import org.jetbrains.compose.compose

plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("common-plugin")
}



kotlin {
	setupJvm()
	
	dependencies {
		implementation(libs.coroutinesAndroid)
		
		implementation(libs.serializationCore)
		implementation(libs.serializationJson)
		
		implementation(libs.immutableCollections)
		
		api(compose.runtime)
	}
}
