import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer
import extension.implementationBundle

plugins {
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.library.conventions)
    alias(libs.plugins.android.publish.library)
}

android {
    namespace = "com.raxdenstudios.platform.notification"
}

versioning {
    filePath = "version.properties"
}

publishLibrary {
    name = "Android Platform Notification"
    description = "Android Platform Notification"
    url = "https://github.com/raxden/android-platform-notification"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-notification")
}

dependencies {
    implementation(projects.platform.core)

    implementation(platform(libs.commons.bom))
    implementation(libs.commons.android)
    implementation(libs.commons.core)
    implementation(libs.commons.coroutines)

    // api: NotificationPermissionLauncher.attach takes a ComponentActivity.
    api(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementationBundle(libs.bundles.koin)

    testImplementation(libs.commons.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.mockk.android)
    testImplementation(libs.robolectric)
}
