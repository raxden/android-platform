import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.device"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform Device"
    description = "Android Platform Device"
    url = "https://github.com/raxden/android-platform-device"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-device")
}

dependencies {
    implementation(projects.platform.core)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.android)
    implementation(libs.commons.coroutines)
    implementation(libs.commons.permissions)

    implementation(libs.play.services.location)

    implementationBundle(libs.bundles.firebase)
    implementationBundle(libs.bundles.koin)
}
