import com.raxdenstudios.publishing.model.Coordinates
import com.raxdenstudios.publishing.model.Developer

plugins {
    `java-platform`
    alias(libs.plugins.android.versioning)
    alias(libs.plugins.android.publish.platform)
}

versioning {
    filePath = "version.properties"
}

publishPlatform {
    name = "Android Platform BOM"
    description = "Bill of Materials for Android Platform libraries"
    url = "https://github.com/raxden/android-platform"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "platform-bom")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    // If anyone use these dependencies, then are added
    constraints {
        api(projects.platform.core)
        api(projects.platform.ui)
        api(projects.platform.device)
        api(projects.platform.network)

        api(projects.component.rating)
        api(projects.component.permission)
    }
}
