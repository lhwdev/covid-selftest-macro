// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
	val kotlinVersion = "1.5.10" // compose + serialization
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:7.1.0-alpha02")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
		classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
		classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
		
		// NOTE: Do not place your application dependencies here; they belong
		// in the individual module build.gradle files
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
