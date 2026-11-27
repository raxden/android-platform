package com.raxdenstudios.platform.featureflag.domain.model

internal enum class TestFeatureFlag(
    override val key: String,
    override val defaultValue: Boolean,
) : FeatureFlag {

    PODCASTS(
        key = "podcasts_enabled",
        defaultValue = false,
    ),
    DARK_MODE(
        key = "dark_mode_enabled",
        defaultValue = true,
    ),
}
