// Top-level build file where you can add configuration options common to all sub-projects/modules.

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
