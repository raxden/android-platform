package com.raxdenstudios.platform.device.domain.model

/** What the installed version must do according to the remote [AppUpdatePolicy]. */
sealed interface AppUpdateRequirement {

    /** The installed version satisfies every threshold. */
    data object None : AppUpdateRequirement

    /** The installed version is below [AppUpdatePolicy.recommendedVersionCode]; updating is optional. */
    data class Optional(
        val installedVersionCode: Long,
        val recommendedVersionCode: Long,
    ) : AppUpdateRequirement

    /** The installed version is below [AppUpdatePolicy.minVersionCode]; the app must not be used until updated. */
    data class Required(
        val installedVersionCode: Long,
        val minVersionCode: Long,
    ) : AppUpdateRequirement
}
