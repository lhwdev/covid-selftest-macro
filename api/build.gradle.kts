plugins {
	id("kotlin")
	id("kotlinx-serialization")
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.1.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
}
