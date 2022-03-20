@file:Suppress("UnstableApiUsage")

// includeBuild is a separate gradle project included by Composite build, so we need to repeat this here.

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../gradle/libs.versions.toml"))
		}
	}
}
