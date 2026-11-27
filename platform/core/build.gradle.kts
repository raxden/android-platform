import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.core"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform Core"
    description = "Android Platform Core"
    url = "https://github.com/raxden/android-platform-core"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-core")
}

dependencies {
    api(platform(libs.commons.bom))
    api(libs.commons.android)
    implementation(libs.commons.core)
    implementation(libs.commons.coroutines)
    // api: the public DIInitializer implements Initializer, so apps that reference it need it too.
    api(libs.androidx.startup)

    implementation(libs.play.services.location)
    implementation(libs.play.review.ktx)
    implementation(libs.coroutines.play.services)

    implementationBundle(libs.bundles.android.material)
    implementationBundle(libs.bundles.android.compose)
    implementationBundle(libs.bundles.room)
    implementationBundle(libs.bundles.koin)

    testImplementation(libs.commons.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.mockk.android)
}
