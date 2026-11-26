import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.featureflag"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform Feature Flag"
    description = "Android Platform Feature Flag"
    url = "https://github.com/raxden/android-platform-featureflag"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-featureflag")
}

dependencies {
    implementation(projects.platform.core)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.core)
    implementation(libs.commons.coroutines)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    implementationBundle(libs.bundles.koin)

    testImplementation(libs.commons.coroutines.test)
    testImplementation(libs.mockk.android)
}
