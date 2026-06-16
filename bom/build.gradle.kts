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
    name = "Android Components BOM"
    description = "Bill of Materials for Android Components libraries"
    url = "https://github.com/raxden/android-components"
    developer = Developer(
        id = "raxden",
        name = "Ángel Gómez",
        email = "raxden.dev@gmail.com",
    )
    coordinates = Coordinates.default.copy(artifactId = "components-bom")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    // If anyone use these dependencies, then are added
    constraints {
        api(projects.component.permission)
        api(projects.component.rating)
    }
}
