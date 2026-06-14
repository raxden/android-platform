package extension

import model.Versions
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = getVersionCatalog()

private fun Project.getVersionCatalog(
    name: String = "libs",
): VersionCatalog = extensions
    .getByType<VersionCatalogsExtension>()
    .named(name)

internal fun VersionCatalog.findLibraryOrThrow(
    name: String
): Provider<MinimalExternalModuleDependency> = findLibrary(name)
    .orElseThrow { NoSuchElementException("Library $name not found in version catalog") }

internal fun VersionCatalog.findVersionOrThrow(
    name: String
): String = findVersion(name)
    .orElseThrow { NoSuchElementException("Version $name not found in version catalog") }
    .requiredVersion

internal val VersionCatalog.versions: Versions
    get() = Versions(this)
