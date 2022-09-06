import com.lhwdev.build.*
import org.jetbrains.compose.compose

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	// `maven-publish`
	// id("com.github.johnrengelman.shadow") version "7.1.2"
	
	id("common-plugin")
}

kotlin {
	explicitApi()
}

dependencies {
	implementation(projects.api)
	implementation(projects.apiBase)
	implementation(projects.transkey)
	implementation(projects.utils)
	
	implementation(libs.coroutinesCore)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(compose.runtime)
}
