import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.network"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform Network"
    description = "Android Platform Network"
    url = "https://github.com/raxden/android-platform-network"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-network")
}

dependencies {
    implementation(projects.platform.core)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.android)
    implementation(libs.commons.threetenabp)
    implementation(libs.commons.network)

    implementation(libs.play.services.location)

    implementationBundle(libs.bundles.android.material)
    implementationBundle(libs.bundles.koin)

    // debug libraries
    debugImplementation(libs.chucker.debug)

    // release libraries
    releaseImplementation(libs.chucker.release)
}
