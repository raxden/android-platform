import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.library.conventions)
    alias(libs.plugins.android.publish.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.raxdenstudios.platform.engagement"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform Engagement"
    description = "Android Platform Engagement"
    url = "https://github.com/raxden/android-platform-engagement"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-engagement")
}

dependencies {
    implementation(projects.platform.core)
    implementation(projects.platform.device)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.core)
    implementation(libs.commons.android)
    implementation(libs.commons.coroutines)

    implementation(libs.kotlinx.serialization.json)

    implementationBundle(libs.bundles.koin)

    testImplementation(libs.commons.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.mockk.android)
}
