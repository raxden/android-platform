package com.raxdenstudios.platform.engagement.data.datasource.local.install

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber

/**
 * Epoch millis when this app was first installed on the device (`PackageInfo.firstInstallTime`),
 * kept across updates. Returns `0` (after logging the error) when the package info cannot be read,
 * which never happens for the app's own package; the engagement prompts are then not delayed by
 * the install age.
 */
internal fun Context.firstInstallTimeMillis(): Long = runCatching { ownPackageInfo().firstInstallTime }
    .onFailure { error -> Timber.w(error, "Could not read the first install time") }
    .getOrDefault(0L)

private fun Context.ownPackageInfo(): PackageInfo = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))

    else ->
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
}
