dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            val tomlFilePath = listOf(
                "${rootProject.projectDir.parentFile}/gradle/libraries.versions.toml",
                "./gradle/libraries.versions.toml",
            )
            for (path in tomlFilePath) {
                val file = File(path)
                if (file.exists()) {
                    from(files(file))
                    break
                }
            }
        }
    }
}
include(":convention")
