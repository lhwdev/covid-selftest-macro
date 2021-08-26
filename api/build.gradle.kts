import java.util.Properties


plugins {
	`maven-publish`
	id("com.github.johnrengelman.shadow") version "7.0.0"
	id("kotlin")
	id("kotlinx-serialization")
}


publishing {
	val config = Properties()
	val file = rootProject.file("local.properties")
	if(file.exists()) file.inputStream().use { config.load(it) }
	val keyPrefix = "publishMaven.api."
	
	publications {
		create<MavenPublication>("covid-hcs") {
			groupId = "com.lhwdev.covid-hcs"
			artifactId = "hcs-api"
			version = "1.0.0"
			
			pom {
				licenses {
					license {
						name.set("Apache License 2.0")
						url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}
			}
			
			from(components["java"])
		}
	}
	
	repositories {
		maven {
			url = uri(config[keyPrefix + "url"].toString())
			credentials {
				username = config[keyPrefix + "username"].toString()
				password = config[keyPrefix + "password"].toString()
			}
		}
	}
}

dependencies {
	implementation(project(":api-base"))
	implementation(project(":transkey"))
	
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
}
