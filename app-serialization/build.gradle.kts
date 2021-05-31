plugins {
	id("com.android.library")
	kotlin("android")
	id("androidx.navigation.safeargs")
	id("kotlinx-serialization")
}

android {
	compileSdk = 30
	buildToolsVersion = "30.0.2"
	
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
	
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")
	testImplementation("junit:junit:4.13.1")
	androidTestImplementation("androidx.test.ext:junit:1.1.2")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
