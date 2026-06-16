import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.compose.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.component.rating"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Rating Component"
    description = "Android Rating Component"
    url = "https://github.com/raxden/android-component-rating"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "component-rating")
}

dependencies {
    implementation(projects.platform.core)
    implementation(projects.platform.ui)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.android.compose)
    implementation(libs.commons.coroutines)
    implementation(libs.commons.threetenabp)

    implementation(libs.play.services.location)
    implementation(libs.play.review.ktx)
    implementation(libs.coroutines.play.services)

    implementationBundle(libs.bundles.android.material)
    implementationBundle(libs.bundles.android.compose)
    implementationBundle(libs.bundles.room)
    implementationBundle(libs.bundles.koin)
}
