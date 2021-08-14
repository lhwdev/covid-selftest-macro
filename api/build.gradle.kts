plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
}

kotlin {
	explicitApi()
	target.compilations.all { kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.RequiresOptIn" }
}

dependencies {
	implementation(project(":api-base"))
	implementation(project(":transkey"))
	
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.2")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
}
