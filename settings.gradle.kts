rootProject.name = "SelfTest-Macro"
include(":app", ":app-base", ":app-models", ":utils", ":transkey", ":api-base", ":api", ":test")


pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
	}
	
	resolutionStrategy.eachPlugin {
		when(requested.id.id) {
			"com.android.application", "com.android.library" ->
				useModule("com.android.tools.build:gradle:7.0.0")
			
			else -> Unit
		}
	}
}
