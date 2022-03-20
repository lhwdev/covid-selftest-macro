plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	
	// `maven-publish`
	// id("com.github.johnrengelman.shadow") version "7.1.2"
	
	id("common-plugin")
}

/*
java {
	withJavadocJar()
	withSourcesJar()
}

operator fun NodeList.iterator(): Iterator<Node> = object : Iterator<Node> {
	private var index = 0
	override fun hasNext(): Boolean = index < length
	override fun next(): Node = item(index++) ?: error("no more element")
}

fun NodeList.iterable(): Iterable<Node> = object : Iterable<Node> {
	override fun iterator(): Iterator<Node> = this@iterable.iterator()
}


// https://github.com/lhwdev/covid-selftest-macro/packages
// See also: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry
publishing {
	repositories {
		maven {
			name = "eduro-hcs-api"
			url = uri("https://maven.pkg.github.com/lhwdev/covid-selftest-macro")
			credentials {
				val properties = Properties()
				project.rootProject.file("local.properties").inputStream().use { properties.load(it) }
				
				username = properties.getProperty("gpr.user") ?: System.getenv("GH_USERNAME")
				password = properties.getProperty("gpr.key") ?: System.getenv("GH_TOKEN")
			}
		}
	}
	
	publications.register<MavenPublication>("gpr") {
		groupId = "com.lhwdev.selfTestMacro"
		artifactId = "hcs-api"
		version = "1.0.0-TEST.04"
		
		val component = components["java"] as org.gradle.api.plugins.internal.DefaultAdhocSoftwareComponent
		component.usages.forEach { usage ->
			val iterator = usage.dependencies.iterator()
			while(iterator.hasNext()) {
				val dependency = iterator.next()
				println(dependency)
				if(dependency is ProjectDependency) {
					println("remove $dependency (${dependency.name}")
					iterator.remove()
				}
			}
		}
		
		from(component)
		val jar = artifacts.find { it.extension == "jar" && it.classifier == "all" }!!
		println("remove artifact $jar")
		artifacts -= jar
		artifact(project.tasks.named("shadowJar"))
		println("resulting $artifacts")
		// error("todo")
		pom {
			name.set("eduro-hcs API")
			description.set("An API implementation backend for eduro-hcs (https://hcs.eduro.go.kr)")
			properties.set(
				mapOf(
					// A checked valid version of hcs ui (on menu: 'UI ver. 1.7.85)
					// This api can work well with, or cannot work with later version than this.
					"valid-hcs-ui-version" to "1.7.85",
					
					// The epoch unix timestamp of when I checked if this works.
					// Just additional information.
					"valid-hcs-checked-at" to "1641137445"
				)
			)
			
			licenses {
				license {
					name.set("The Apache License, Version 2.0")
					url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
				}
			}
			
			developers {
				developer {
					id.set("lhwdev")
					name.set("lhwdev")
					email.set("lhwdev6@outlook.com")
				}
			}
			
			scm {
				connection.set("scm:git:git://github.com/lhwdev/covid-selftest-macro.git")
				url.set("https://github.com/lhwdev/covid-selftest-macro")
			}
		}
		
		// project.shadow.component(this)
		// if (GradleVersion.current() >= GradleVersion.version("6.6")) {
		//     publication.artifact(project.tasks.named("shadowJar"))
		// } else {
		//     publication.artifact(project.tasks.shadowJar)
		// }
		// 
		// publication.pom { MavenPom pom ->
		//     pom.withXml { xml ->
		//         def dependenciesNode = xml.asNode().appendNode('dependencies')
		// 
		//         project.configurations.shadow.allDependencies.each {
		//             if ((it instanceof ProjectDependency) || ! (it instanceof SelfResolvingDependency)) {
		//                 def dependencyNode = dependenciesNode.appendNode('dependency')
		//                 dependencyNode.appendNode('groupId', it.group)
		//                 dependencyNode.appendNode('artifactId', it.name)
		//                 dependencyNode.appendNode('version', it.version)
		//                 dependencyNode.appendNode('scope', 'runtime')
		//             }
		//         }
		//     }
		// }
		
		// Hacky but only option
		// pom.withXml {
			// val dependencies = asElement().childNodes.iterable().find { it.nodeName == "dependencies" }!!
			// val filtered = project.configurations.runtimeClasspath.get().dependencies
			// 	.filterIsInstance<ProjectDependency>()
			// val nodesToFilter = mutableListOf<Node>()
			//
			// for(dependency in dependencies.childNodes) {
			// 	var groupId: String? = null
			// 	var artifactId: String? = null
			// 	var version: String? = null
			//	
			// 	for(node in dependency.childNodes) when(node.nodeName) {
			// 		"groupId" -> groupId = node.textContent
			// 		"artifactId" -> artifactId = node.textContent
			// 		"version" -> version = node.textContent
			// 	}
			//	
			// 	if(groupId == null || artifactId == null || version == null) continue
			//	
			// 	if(filtered.any { it.group == groupId && it.name == artifactId && it.version == version }) {
			// 		nodesToFilter += dependency
			// 	}
			// }
			//
			// for(toFilter in nodesToFilter) dependencies.removeChild(toFilter)
		// }
	}
}

tasks.javadoc {
	if(JavaVersion.current().isJava9Compatible) {
		(options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
	}
}

tasks.named<ShadowJar>("shadowJar") {
	dependencies {
		include { it is ProjectDependency }
	}
}
*/
kotlin {
	explicitApi()
	target.compilations.all { kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn" }
}

dependencies {
	implementation(projects.apiBase)
	implementation(projects.transkey)
	
	implementation(libs.coroutinesAndroid)
	
	implementation(libs.serializationCore)
	implementation(libs.serializationJson)
}
