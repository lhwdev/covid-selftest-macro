plugins {
	id("com.android.library")
	kotlin("android")
	id("kotlinx-serialization")
}

android {
	compileSdk = 30
	
	defaultConfig {
		minSdk = 19
		targetSdk = 30
	}
	
	
	buildTypes {
		named("release") {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	
	kotlinOptions {
		jvmTarget = "1.8"
	}
}


dependencies {
	implementation(project(":api"))
	
	implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
	implementation(kotlin("stdlib"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")
	
	
	val compose = "1.0.0" // also kotlinCompilerExtensionVersion, app-serialization/version
	implementation("androidx.compose.runtime:runtime:$compose")
	
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
	testImplementation("junit:junit:4.13.2")
	androidTestImplementation("androidx.test.ext:junit:1.1.3")
	// androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
	
	implementation("androidx.core:core-ktx:1.6.0")
}
