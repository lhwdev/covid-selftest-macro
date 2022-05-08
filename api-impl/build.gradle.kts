import com.lhwdev.build.*

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	
	// `maven-publish`
	// id("com.github.johnrengelman.shadow") version "7.1.2"
	
	id("common-plugin")
}

kotlin {
	explicitApi()
	setup()
}

dependencies {
	implementation(projects.api)
	implementation(projects.apiBase)
	implementation(projects.transkey)
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
}
