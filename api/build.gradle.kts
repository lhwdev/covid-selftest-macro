import com.lhwdev.build.*
import org.jetbrains.compose.compose

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("common-plugin")
}

kotlin {
	explicitApi()
}

dependencies {
	implementation(projects.utils)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(compose.runtime)
}
