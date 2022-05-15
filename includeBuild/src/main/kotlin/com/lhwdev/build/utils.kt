package com.lhwdev.build

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget


// temporary

val ComposePlugin.Dependencies.foundationLayout
	get() = "org.jetbrains.compose.foundation:foundation:${ComposeBuildConfig.composeVersion}"

// all common

fun KotlinProjectExtension.setupCommon() {
	sourceSets {
		all {
			languageSettings.apply {
				enableLanguageFeature("InlineClasses")
				optIn("kotlin.RequiresOptIn")
				optIn("kotlin.ExperimentalUnsignedTypes")
				optIn("androidx.compose.material.ExperimentalMaterialApi")
				optIn("androidx.compose.ui.ExperimentalComposeUiApi")
				optIn("androidx.compose.animation.ExperimentalAnimationApi")
			}
		}
		
		val testSourceSet = if(this@setupCommon is KotlinMultiplatformExtension) "commonTest" else "test"
		
		named(testSourceSet) {
			dependencies {
				implementation(kotlin("test-common"))
				implementation(kotlin("test-annotations-common"))
			}
		}
	}
}


// jvm

private fun KotlinProjectExtension.setupJvmCommon(name: String?) {
	sourceSets {
		named(sourceSetNameFor(name, "test")) {
			dependencies {
				implementation(kotlin("test-junit"))
			}
		}
	}
}

fun KotlinJvmProjectExtension.setup(init: (KotlinSetup<KotlinWithJavaTarget<KotlinJvmOptions>>.() -> Unit)? = null) {
	setupCommon()
	setupJvmCommon(null)
	
	target.compilations.all {
		kotlinOptions.jvmTarget = "1.8"
	}
	
	init?.invoke(KotlinSetup(target, null, sourceSets))
}


// mpp

fun KotlinMultiplatformExtension.dependencies(name: String = "commonMain", block: KotlinDependencyHandler.() -> Unit) {
	sourceSets {
		named(name) {
			dependencies(block)
		}
	}
}

fun KotlinMultiplatformExtension.library() {
	setupCommon()
	setupJvm()
	// js {
	// 	browser()
	// 	nodejs()
	// }
	
	// val hostOs = System.getProperty("os.name")
	// val isMingwX64 = hostOs.startsWith("Windows")
	// when {
	// 	hostOs == "Mac OS X" -> macosX64("native")
	// 	hostOs == "Linux" -> linuxX64("native")
	// 	isMingwX64 -> mingwX64("native")
	// 	else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
	// }
}

fun KotlinMultiplatformExtension.setupJvm(
	name: String = "jvm",
	init: (KotlinSetup<KotlinJvmTarget>.() -> Unit)? = null
): KotlinJvmTarget {
	val target = jvm(name) {
		compilations.all {
			kotlinOptions.jvmTarget = "1.8"
		}
	}
	
	setupJvmCommon(name)
	init?.invoke(KotlinSetup(target, name, sourceSets))
	return target
}

fun KotlinMultiplatformExtension.setupJs(
	name: String = "js",
	init: (KotlinSetup<KotlinJsTargetDsl>.() -> Unit)? = null
): KotlinJsTargetDsl {
	val target = js(name)
	
	sourceSets {
		named("${name}Test") {
			dependencies {
				implementation(kotlin("test-js"))
			}
		}
	}
	
	init?.invoke(KotlinSetup(target, name, sourceSets))
	return target
}

@Suppress("UnstableApiUsage")
fun CommonExtension<*, *, *, *>.setupCommon() {
	compileSdk = 31
	
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
}

// fun KotlinMultiplatformExtension.setupAndroid(
// 	project: Project,
// 	name: String = "android",
// 	init: (KotlinSetup<KotlinAndroidTarget>.() -> Unit)? = null
// ): KotlinAndroidTarget {
// 	val target = android(name) {
// 		compilations.all {
// 			kotlinOptions.jvmTarget = "1.8"
// 		}
// 	}
//
// 	setupJvmCommon(name)
// 	init?.invoke(KotlinSetup(target, name, sourceSets))
//
// 	project.extensions.getByType<BaseExtension>().sourceSets.all {
// 		val directory = "src/$name${this.name.firstToUpperCase()}"
// 		setRoot(directory)
// 	}
//
// 	return target
// }


private fun String.firstToUpperCase() = replaceRange(0, 1, first().toUpperCase().toString())

private fun sourceSetNameFor(name: String?, type: String) =
	if(name == null) type else "$name${type.firstToUpperCase()}"


class KotlinSetup<Target : KotlinTarget>(
	val target: Target,
	val name: String?,
	val sourceSet: NamedDomainObjectContainer<KotlinSourceSet>
) {
	fun target(block: Target.() -> Unit) {
		target.block()
	}
	
	private fun dependencies(type: String, block: KotlinDependencyHandler.() -> Unit) {
		sourceSet.named(sourceSetNameFor(name, type)) {
			dependencies(block)
		}
	}
	
	fun dependencies(block: KotlinDependencyHandler.() -> Unit) {
		dependencies("main", block)
	}
	
	fun testDependencies(block: KotlinDependencyHandler.() -> Unit) {
		dependencies("test", block)
	}
}
