package com.raxdenstudios.platform.ui.extension

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

@Stable
fun Modifier.mirror(): Modifier = this.scale(scaleX = -1f, scaleY = 1f)

fun Modifier.aspectRatio11() = aspectRatio(ASPECT_RATIO_1_1)

fun Modifier.aspectRatio43() = aspectRatio(ASPECT_RATIO_4_3)

fun Modifier.aspectRatio169() = aspectRatio(ASPECT_RATIO_16_9)

fun Modifier.aspectRatio23() = aspectRatio(ASPECT_RATIO_2_3)

private const val ASPECT_RATIO_16_9 = 16f / 9f
private const val ASPECT_RATIO_4_3 = 4f / 3f
private const val ASPECT_RATIO_2_3 = 2f / 3f
private const val ASPECT_RATIO_1_1 = 1f / 1f
