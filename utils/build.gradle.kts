import com.lhwdev.build.*
import org.jetbrains.compose.compose

plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("common-plugin")
}



commonConfig {
	kotlin {
		jvm {
			dependencies {
				implementation(libs.coroutinesCore)
			}
		}
		
		dependencies {
			implementation(libs.coroutinesCore)
			
			implementation(libs.serializationCore)
			implementation(libs.serializationJson)
			
			api(compose.runtime)
		}
	}
}
