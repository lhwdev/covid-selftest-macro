import com.lhwdev.build.*
import org.jetbrains.compose.compose

plugins {
	id("com.android.library")
	kotlin("android")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("common-plugin")
}


kotlin {
	setupCommon()
}

android {
	setupCommon()
	
	defaultConfig {
		minSdk = 21
		targetSdk = 31
	}
}


dependencies {
	implementation(projects.apiBase)
	implementation(projects.api)
	implementation(projects.utils)
	
	implementation("net.gotev:cookie-store:1.5.0")
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(libs.immutableCollections)
	
	implementation(compose.ui)
	implementation(compose.uiTooling)
	implementation(compose.foundation)
	implementation(compose.foundationLayout)
	implementation(compose.material)
	
	implementation(libs.androidx.activity.compose)
	
	// implementation(libs.accompanist.insetsUi)
	implementation(libs.accompanist.systemUiController)
	
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.coreKtx)
}
