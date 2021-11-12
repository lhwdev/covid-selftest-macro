plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
}

kotlin {
	explicitApi()
	target.compilations.all { kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn" }
}

dependencies {
	implementation(project(":api-base"))
	implementation(project(":transkey"))
	
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
}
