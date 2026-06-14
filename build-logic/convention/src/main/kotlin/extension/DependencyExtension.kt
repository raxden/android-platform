package extension

import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.implementationBundle(
    dependencyNotation: Provider<ExternalModuleDependencyBundle>
) {
    dependencyNotation.get().forEach { dependency ->
        if (dependency.name.contains("bom")) {
            add("implementation", platform(dependency))
        } else if (dependency.name.contains("compiler")) {
            add("ksp", dependency)
        } else {
            add("implementation", dependency)
        }
    }
}

fun DependencyHandlerScope.debugImplementationBundle(
    dependencyNotation: Provider<ExternalModuleDependencyBundle>
) {
    dependencyNotation.get().forEach { dependency ->
        if (dependency.name.contains("bom")) {
            add("debugImplementation", platform(dependency))
        } else {
            add("debugImplementation", dependency)
        }
    }
}

fun DependencyHandlerScope.androidTestImplementationBundle(
    dependencyNotation: Provider<ExternalModuleDependencyBundle>
) {
    dependencyNotation.get().forEach { dependency ->
        if (dependency.name.contains("bom")) {
            add("androidTestImplementation", platform(dependency))
        } else if (dependency.name.contains("compiler")) {
            add("kspAndroidTest", dependency)
        } else {
            add("androidTestImplementation", dependency)
        }
    }
}
