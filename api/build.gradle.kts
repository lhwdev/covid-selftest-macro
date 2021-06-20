plugins {
	id("kotlin")
	id("kotlinx-serialization")
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
}
