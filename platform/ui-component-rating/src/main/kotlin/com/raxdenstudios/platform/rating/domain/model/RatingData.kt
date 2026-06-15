package com.raxdenstudios.platform.rating.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RatingData(
    val firstInstallTimeMs: Long = System.currentTimeMillis(),
    val hasRated: Boolean = false,
    val dismissCount: Int = 0,
    val lastDismissTimeMs: Long = 0L,
)