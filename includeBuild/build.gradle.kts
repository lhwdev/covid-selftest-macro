plugins {
	`kotlin-dsl`
	`java-gradle-plugin`
}


group = "com.lhwdev.include-build"

repositories {
	mavenCentral()
	google()
}

gradlePlugin {
	plugins.register("common-plugin") {
		id = "common-plugin"
		implementationClass = "com.lhwdev.build.CommonPlugin"
	}
}

dependencies {
	compileOnly(libs.versions.kotlin.map { "org.jetbrains.kotlin:kotlin-gradle-plugin:$it" })
	compileOnly("com.android.tools.build:gradle:7.0.1")
}
