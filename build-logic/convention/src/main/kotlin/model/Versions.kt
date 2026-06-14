package model

import extension.findVersionOrThrow
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.jvm.toolchain.JavaLanguageVersion

class Versions(
    catalog: VersionCatalog
) {
    val minSdk: Int = catalog.findVersionOrThrow("minSdk").toInt()
    val compileSdk: Int = catalog.findVersionOrThrow("compileSdk").toInt()
    val targetSdk: Int = catalog.findVersionOrThrow("targetSdk").toInt()
    val sourceCompatibility: JavaVersion =
        JavaVersion.toVersion(catalog.findVersionOrThrow("sourceCompatibility"))
    val targetCompatibility: JavaVersion =
        JavaVersion.toVersion(catalog.findVersionOrThrow("targetCompatibility"))
    val jdk: JavaLanguageVersion =
        JavaLanguageVersion.of(catalog.findVersionOrThrow("jdk"))
}
