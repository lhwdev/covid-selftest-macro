import com.lhwdev.build.*

plugins {
	id("com.android.library")
	kotlin("android")
	kotlin("plugin.serialization")
	
	id("common-plugin")
}


android {
	setupCommon()
	
	defaultConfig {
		minSdk = 21
		targetSdk = 31
	}
	
	composeOptions {
		kotlinCompilerExtensionVersion = libs.versions.compose.get()
	}
}


dependencies {
	implementation(projects.appModels)
	implementation(projects.apiBase)
	implementation(projects.api)
	implementation(projects.utils)
	
	implementation("net.gotev:cookie-store:1.4.0")
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
	
	implementation(libs.immutableCollections)
	
	implementation(libs.compose.ui)
	implementation(libs.compose.uiTooling)
	implementation(libs.compose.foundation)
	implementation(libs.compose.foundationLayout)
	implementation(libs.compose.material)
	
	implementation(libs.androidx.activity.compose)
	
	// implementation(libs.accompanist.insetsUi)
	implementation(libs.accompanist.systemUiController)
	
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.coreKtx)
}
