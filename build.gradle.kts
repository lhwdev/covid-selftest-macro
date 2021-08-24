plugins {
	val kotlinVersion = "1.5.21" // compose + serialization
	
	id("com.android.application") version "4.2.2" apply false
	
	kotlin("multiplatform") version kotlinVersion apply false
	kotlin("jvm") version kotlinVersion apply false
	kotlin("android") version kotlinVersion apply false
	kotlin("plugin.serialization") version kotlinVersion apply false
}


buildscript {
	repositories {
		google()
		mavenCentral()
	}
}

allprojects {
	repositories {
		google()
		mavenCentral()
	}
}

tasks.register<Delete>("clean") {
	delete(rootProject.buildDir)
}
