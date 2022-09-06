package com.lhwdev.build

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget


abstract class KotlinScope(internal val commonConfig: CommonConfig) {
	internal abstract val kotlin: KotlinProjectExtension
	
	internal open fun setup() {
		kotlin.sourceSets {
			all {
				languageSettings.apply {
					enableLanguageFeature("InlineClasses")
					optIn("kotlin.RequiresOptIn")
					optIn("kotlin.ExperimentalUnsignedTypes")
				}
			}
			
			val testSourceSet = if(kotlin is KotlinMultiplatformExtension) "commonTest" else "test"
			
			named(testSourceSet) {
				dependencies {
					implementation(kotlin("test-common"))
					implementation(kotlin("test-annotations-common"))
				}
			}
		}
	}
}

open class KotlinMultiplatformScope(commonConfig: CommonConfig, override val kotlin: KotlinMultiplatformExtension) :
	KotlinScope(commonConfig) {
	
	val common: KotlinCommonItem = KotlinCommonItem(sourceSet = KotlinTargetSourceSet(kotlin.sourceSets, "common"))
	
	
	fun intermediate(name: String, setupBlock: KotlinIntermediateItem.() -> Unit = {}): KotlinIntermediateItem {
		val item = KotlinIntermediateItem(
			name = name,
			sourceSet = KotlinTargetSourceSet(
				main = kotlin.sourceSets.create(sourceSetNameFor(name, type = "main")),
				test = null
				// test = kotlin.sourceSets.create(sourceSetNameFor(name, type = "test"))
			)
		)
		
		item.setupBlock()
		
		return item
	}
	
	fun jvm(name: String = "jvm", setupBlock: KotlinJvmItem.() -> Unit = {}): KotlinJvmItem {
		val target = kotlin.jvm(name)
		val item = KotlinJvmItem(
			target = target,
			sourceSet = KotlinTargetSourceSet(kotlin.sourceSets, name = name)
		)
		
		item.setupBlock()
		
		return item
	}
	
	fun dependencies(block: KotlinDependencyHandler.() -> Unit) {
		common.dependencies(block)
	}
	
	fun testDependencies(block: KotlinDependencyHandler.() -> Unit) {
		common.testDependencies(block)
	}
}

abstract class KotlinPlatformScope(commonConfig: CommonConfig) : KotlinScope(commonConfig) {
	protected abstract val platformItem: KotlinPlatformItem
	
	fun dependencies(block: KotlinDependencyHandler.() -> Unit) {
		platformItem.dependencies(block)
	}
	
	fun testDependencies(block: KotlinDependencyHandler.() -> Unit) {
		platformItem.testDependencies(block)
	}
}

abstract class KotlinJvmKindScope(commonConfig: CommonConfig, kotlin: KotlinSingleTargetExtension) :
	KotlinPlatformScope(commonConfig) {
	
	abstract override val kotlin: KotlinSingleTargetExtension
	
	val jvm: KotlinJvmItem = KotlinJvmItem(
		target = kotlin.target,
		sourceSet = KotlinTargetSourceSet(kotlin.sourceSets, name = null)
	)
	
	override val platformItem: KotlinPlatformItem
		get() = jvm
	
	override fun setup() {
		kotlin.sourceSets {
			named("test") {
				dependencies {
					implementation(kotlin("test-junit"))
				}
			}
		}
	}
}

open class KotlinJvmScope(commonConfig: CommonConfig, final override val kotlin: KotlinJvmProjectExtension) :
	KotlinJvmKindScope(commonConfig, kotlin)


class KotlinTargetSourceSet(val main: KotlinSourceSet, val test: KotlinSourceSet?) {
	constructor(sourceSets: NamedDomainObjectContainer<KotlinSourceSet>, name: String?) : this(
		main = sourceSets[sourceSetNameFor(name, "main")],
		test = sourceSets[sourceSetNameFor(name, "test")]
	)
}


interface KotlinItem {
	fun dependencies(block: KotlinDependencyHandler.() -> Unit)
	
	fun testDependencies(block: KotlinDependencyHandler.() -> Unit)
}

interface KotlinDependencyItem : KotlinItem {
	val dependencySourceSet: KotlinTargetSourceSet
}

interface KotlinDependantItem : KotlinItem {
	fun dependsOn(item: KotlinDependencyItem)
}


abstract class AbstractKotlinItem : KotlinItem {
	protected abstract val targetSourceSet: KotlinTargetSourceSet
	
	fun dependsOn(item: KotlinDependencyItem) {
		targetSourceSet.main.dependsOn(item.dependencySourceSet.main)
	}
	
	override fun dependencies(block: KotlinDependencyHandler.() -> Unit) {
		targetSourceSet.main.dependencies(block)
	}
	
	override fun testDependencies(block: KotlinDependencyHandler.() -> Unit) {
		targetSourceSet.test!!.dependencies(block)
	}
}

abstract class KotlinTargetItem(internal val sourceSet: KotlinTargetSourceSet) : AbstractKotlinItem() {
	override val targetSourceSet: KotlinTargetSourceSet get() = sourceSet
}

class KotlinCommonItem(sourceSet: KotlinTargetSourceSet) : KotlinTargetItem(sourceSet = sourceSet),
	KotlinDependencyItem {
	override val dependencySourceSet: KotlinTargetSourceSet get() = sourceSet
}

abstract class KotlinPlatformItem(sourceSet: KotlinTargetSourceSet) : KotlinTargetItem(sourceSet = sourceSet),
	KotlinDependantItem

class KotlinJvmItem(
	val target: KotlinTarget,
	sourceSet: KotlinTargetSourceSet
) : KotlinPlatformItem(sourceSet = sourceSet) {
}


class KotlinIntermediateItem(
	private val name: String,
	internal val sourceSet: KotlinTargetSourceSet
) : AbstractKotlinItem(), KotlinDependencyItem, KotlinDependantItem {
	override val targetSourceSet: KotlinTargetSourceSet get() = sourceSet
	override val dependencySourceSet: KotlinTargetSourceSet get() = sourceSet
}

