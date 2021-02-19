plugins {
	id("com.android.application")
	kotlin("android")
	id("androidx.navigation.safeargs")
}

android {
	compileSdk = 30
	buildToolsVersion = "30.0.2"

	defaultConfig {
		applicationId = "com.lhwdev.selfTestMacro"
		minSdk = 21
		targetSdk = 30
		versionCode = 2000
		versionName = "3.0"

		multiDexEnabled = true

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	buildFeatures {
		compose = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	kotlinOptions {
		languageVersion = "1.4"
		jvmTarget = "1.8"
		useIR = true
	}
}


dependencies {
	val kotlinVersion = "1.4.21"
	implementation(project(":api"))
	implementation(project(":app-serialization")) // workaround for compose + serialization

	implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
	implementation(kotlin("stdlib"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

	val compose = "1.0.0-alpha09"
	implementation("androidx.compose.ui:ui:$compose")
	implementation("androidx.compose.ui:ui-tooling:$compose")
	implementation("androidx.compose.foundation:foundation:$compose")
	implementation("androidx.compose.material:material:$compose")

	implementation("androidx.appcompat:appcompat:1.2.0")
	implementation("androidx.core:core-ktx:1.3.2")
	implementation("com.google.android.material:material:1.2.1")
	implementation("androidx.constraintlayout:constraintlayout:2.0.4")
	implementation("androidx.navigation:navigation-fragment-ktx:2.3.2")
	implementation("androidx.navigation:navigation-ui-ktx:2.3.2")
	testImplementation("junit:junit:4.13.1")
	androidTestImplementation("androidx.test.ext:junit:1.1.2")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
