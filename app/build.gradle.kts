import com.lhwdev.build.*

plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("plugin.serialization")
	
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

// tasks.named("licenseeStableRelease") {
// 	val dummyForLicensee = configurations.create("dummyForLicensee") {
// 		isCanBeConsumed = false
// 	}
//	
// 	// var configuration: Configuration by Delegates.notNull()
// 	@Suppress("UNCHECKED_CAST")
// 	val configuration = this::class.memberProperties.find { it.name == "configuration" }!!
// 		as KMutableProperty1<Task, Configuration>
// 	configuration.isAccessible = true
//	
// 	// fun setClasspath(configuration: Configuration, usage: String)
// 	val setClasspath = this::class.memberFunctions.find { it.name == "setClasspath" }!!
//	
// 	val last = configuration.get(this)
// 	val files = dependencies.factory(last.resolve())
// 	last.dependencies
//	
// 	setClasspath.call(this, dummyForLicensee, "android-classes")
// }

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
	
	composeOptions {
		// kotlinCompilerVersion = "1.5.31"
		kotlinCompilerExtensionVersion = libs.versions.compose.get()
	}
}


dependencies {
	implementation(projects.appBase)
	implementation(projects.appModels)
	implementation(projects.api)
	implementation(projects.apiBase)
	implementation(projects.utils)
	
	implementation(libs.lottieCompose)
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(libs.immutableCollections)
	
	implementation(libs.compose.ui)
	implementation(libs.compose.uiTooling)
	implementation(libs.compose.foundation)
	implementation(libs.compose.foundationLayout)
	implementation(libs.compose.material)
	
	implementation(libs.accompanist.systemUiController)
	implementation(libs.accompanist.pager)
	
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.coreKtx)
	
	implementation(libs.bundles.tests)
}
