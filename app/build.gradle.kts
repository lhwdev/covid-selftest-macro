import com.lhwdev.build.*
import org.jetbrains.compose.compose

plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("app.cash.licensee") version "1.2.0"
	
	id("common-plugin")
}

repositories {
	maven(url = "https://oss.sonatype.org/content/repositories/snapshots") // lottie-compose
}

licensee {
	allow("Apache-2.0")
	allow("EPL-1.0")
	allowUrl("http://www.opensource.org/licenses/bsd-license.php")
	// allow("MIT")
	
	// see https://github.com/airbnb/lottie-android/issues/1865
	allowDependency(groupId = "com.airbnb.android", artifactId = "lottie", version = "4.2.2")
	allowDependency(groupId = "com.airbnb.android", artifactId = "lottie-compose", version = "4.2.2")
}

tasks.register<Copy>("updateLicenses") {
	dependsOn("licenseeStableRelease")
	from(File(project.buildDir, "reports/licensee/release/artifacts.json"))
	into(project.file("src/main/res/raw"))
	rename { "open_source_license.json" }
}

kotlin {
	setupCommon()
}

android {
	setupCommon()
	
	defaultConfig {
		applicationId = "com.lhwdev.selfTestMacro"
		minSdk = 21
		targetSdk = 31
		// 3.0.0-build01 ~ 3.0.0-build03 = 3000
		// 3.0.0-build04 = 3001
		versionCode = 3001
		versionName = "3.0.0-build04"
		
		// multiDexEnabled = true
		
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	
	buildTypes {
		named("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	
	
	flavorDimensions += listOf("lifecycle")
	
	productFlavors {
		register("stable") {
			dimension = "lifecycle"
			manifestPlaceholders["appLabel"] = "@string/app_name"
		}
		register("preview") {
			dimension = "lifecycle"
			applicationIdSuffix = ".preview"
			manifestPlaceholders["appLabel"] = "@string/app_name_preview"
		}
		register("dev") {
			dimension = "lifecycle"
			isDefault = true
			applicationIdSuffix = ".dev"
			manifestPlaceholders["appLabel"] = "@string/app_name_dev"
		}
	}
}


dependencies {
	implementation(projects.appBase)
	implementation(projects.api)
	implementation(projects.apiBase)
	implementation(projects.utils)
	
	implementation(libs.lottieCompose)
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(libs.immutableCollections)
	
	implementation(compose.ui)
	implementation(compose.uiTooling)
	implementation(compose.foundation)
	implementation(compose.foundationLayout)
	implementation(compose.material)
	
	implementation(libs.accompanist.systemUiController)
	implementation(libs.accompanist.pager)
	
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.coreKtx)
	
	implementation(libs.bundles.tests)
}
