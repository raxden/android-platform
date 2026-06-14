package com.raxdenstudios.platform.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

private const val ASPECT_RATIO_16_9 = 16f / 9f
private const val ASPECT_RATIO_4_3 = 4f / 3f
private const val ASPECT_RATIO_2_3 = 2f / 3f
private const val ASPECT_RATIO_1_1 = 1f / 1f

fun Modifier.aspectRatio11() = aspectRatio(ASPECT_RATIO_1_1)

fun Modifier.aspectRatio43() = aspectRatio(ASPECT_RATIO_4_3)

fun Modifier.aspectRatio169() = aspectRatio(ASPECT_RATIO_16_9)

fun Modifier.aspectRatio23() = aspectRatio(ASPECT_RATIO_2_3)

@Composable
fun PaddingValues.copy(
    start: Dp = calculateStartPadding(LocalLayoutDirection.current),
    top: Dp = calculateTopPadding(),
    end: Dp = calculateEndPadding(LocalLayoutDirection.current),
    bottom: Dp = calculateBottomPadding(),
): PaddingValues = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom
)
