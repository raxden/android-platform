plugins {
    id("android-library-conventions")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes.apply {
            add("META-INF/AL2.0")
            add("META-INF/LGPL2.1")
        }
    }
}
