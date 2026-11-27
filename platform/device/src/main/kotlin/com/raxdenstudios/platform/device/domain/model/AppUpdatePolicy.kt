package com.raxdenstudios.platform.device.domain.model

import androidx.annotation.VisibleForTesting

/**
 * Version thresholds published remotely to drive in-app updates. A threshold of 0 disables it.
 *
 * @property minVersionCode installs below this `versionCode` must update before using the app.
 * @property recommendedVersionCode installs below this `versionCode` are offered an optional update.
 */
data class AppUpdatePolicy(
    val minVersionCode: Long,
    val recommendedVersionCode: Long,
) {

    companion object {

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val mock = AppUpdatePolicy(
            minVersionCode = 100,
            recommendedVersionCode = 110,
        )
    }
}
