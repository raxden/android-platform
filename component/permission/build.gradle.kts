import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.compose.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.component.permission"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Permission Component"
    description = "Android Permission Component"
    url = "https://github.com/raxden/android-component-permission"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "component-permission")
}

dependencies {
    implementation(projects.platform.core)
    implementation(projects.platform.ui)
    implementation(projects.platform.device)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.android.compose)
    implementation(libs.commons.coroutines)
    implementation(libs.commons.threetenabp)
    implementation(libs.commons.permissions)

    implementation(libs.play.services.location)
    implementation(libs.play.review.ktx)
    implementation(libs.coroutines.play.services)

    implementationBundle(libs.bundles.android.material)
    implementationBundle(libs.bundles.android.compose)
    implementationBundle(libs.bundles.room)
    implementationBundle(libs.bundles.koin)
}
