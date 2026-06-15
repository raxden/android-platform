import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.compose.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.ui.component.permission"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform UI Permission Component"
    description = "Android Platform UI Permission Component"
    url = "https://github.com/raxden/android-platform-ui-permission"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-ui-component-permission")
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
