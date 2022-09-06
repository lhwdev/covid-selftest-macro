@file:Suppress("LeakingThis")

package com.lhwdev.build

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import javax.inject.Inject


/**
 * Not expected to be exhaustive kotlin/etc. configuration utility; created on demand.
 */
open class CommonConfig @Inject constructor(internal val project: Project) {
	init {
		val extensions = (this as ExtensionAware).extensions
		
		val kotlin = project.extensions.findByName("kotlin")
		when(kotlin) {
			null -> null
			is KotlinMultiplatformExtension -> KotlinMultiplatformScope::class.java
			is KotlinJvmProjectExtension -> KotlinJvmScope::class.java
			is KotlinAndroidProjectExtension -> KotlinAndroidScope::class.java
			else -> error("not supported: $kotlin  (${kotlin::class})")
		}?.let {
			val scope = extensions.create("kotlin", it, this, kotlin)
			scope.setup()
		}
		
		initializeAndroid(extensions, project)
	}
}

internal fun String.firstToUpperCase() = replaceRange(0, 1, first().toUpperCase().toString())

internal fun sourceSetNameFor(name: String?, type: String) =
	if(name == null) type else "$name${type.firstToUpperCase()}"
