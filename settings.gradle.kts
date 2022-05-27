rootProject.name = "SelfTest-Macro"

// Type-safe Project Dependency Accessor
// https://docs.gradle.org/7.4/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
		maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
	
	resolutionStrategy.eachPlugin {
		val id = requested.id.id
		
		// Android
		if(id.startsWith("com.android")) { // version: also in includeBuild/build.gradle.kts
			useModule("com.android.tools.build:gradle:${requested.version}")
		}
	}
}


// Projects
includeBuild("includeBuild")

include(
	":app", ":app-base", ":app-models",
	":utils",
	":transkey", ":api-base", ":api-impl", ":api",
	":test"
)
