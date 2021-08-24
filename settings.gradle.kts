rootProject.name = "SelfTest-Macro"
include(":app", ":transkey", ":api-base", ":api", ":test")


pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
	}
	
	resolutionStrategy.eachPlugin {
		when(requested.id.id) {
			"com.android.application", "com.android.library" ->
				useModule("com.android.tools.build:gradle:4.2.2")
			
			else -> Unit
		}
	}
}
