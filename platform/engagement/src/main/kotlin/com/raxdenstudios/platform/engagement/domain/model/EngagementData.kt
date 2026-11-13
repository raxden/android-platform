package com.raxdenstudios.platform.engagement.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EngagementData(
    val hasRated: Boolean = false,
    val hasShared: Boolean = false,
    val counts: Map<String, Int> = emptyMap(),
)
