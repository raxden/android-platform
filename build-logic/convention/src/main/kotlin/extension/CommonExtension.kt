package extension

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import java.util.Properties

fun Project.getProperty(key: String, defaultValue: String = ""): String =
    providers.gradleProperty(key)
        .orElse(providers.environmentVariable(key))
        .getOrElse(defaultValue)

internal fun Project.getSigningConfigProperties(buildType: String): Properties {
    val properties = Properties()
    val propertiesFile = file("$rootDir/config/signing_$buildType.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { properties.load(it) }
    } else {
        println("No signing config found for build type $buildType")
    }
    return properties
}


//fun CommonExtension<*, *, *, *, *, *>.roomSetup(
//    project: Project
//) {
//    val schemasPath = "${project.projectDir}/schemas"
//    defaultConfig {
//        project.kapt {
//            arguments {
//                arg("room.schemaLocation", schemasPath)
//            }
//        }
//    }
//    sourceSets {
//        // Adds exported schema location as test app assets.
//        getByName("debug")
//            .assets
//            .srcDirs(project.files(schemasPath))
//    }
//}

private fun Project.kapt(configure: KaptExtension.() -> Unit): Unit =
    (this as ExtensionAware).extensions.configure("kapt", configure)
