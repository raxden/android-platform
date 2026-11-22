import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.debugImplementationBundle
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.compose.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.ui"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform UI"
    description = "Android Platform UI"
    url = "https://github.com/raxden/android-platform-ui"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-ui")
}

dependencies {
    implementation(projects.platform.core)

    api(platform(libs.commons.bom))
    api(libs.commons.android)
    api(libs.commons.android.compose)
    implementation(libs.commons.core)
    implementation(libs.commons.time)
    implementation(libs.commons.coroutines)

    implementation(libs.play.services.location)
    implementation(libs.play.review.ktx)
    implementation(libs.coroutines.play.services)

    implementationBundle(libs.bundles.android.material)
    implementationBundle(libs.bundles.android.compose)
    implementationBundle(libs.bundles.room)
    implementationBundle(libs.bundles.koin)

    debugImplementationBundle(libs.bundles.debug.android.compose)

    testImplementation(libs.commons.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.mockk.android)
}
