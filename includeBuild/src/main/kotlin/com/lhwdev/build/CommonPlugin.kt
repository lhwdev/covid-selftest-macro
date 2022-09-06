package com.lhwdev.build

import org.gradle.api.Plugin
import org.gradle.api.Project


@Suppress("unused")
class CommonPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		target.extensions.create("commonConfig", CommonConfig::class.java)
	}
}
