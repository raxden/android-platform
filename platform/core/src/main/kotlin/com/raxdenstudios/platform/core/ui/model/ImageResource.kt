package com.raxdenstudios.platform.core.ui.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

sealed interface ImageResource {
    data class Drawable(@DrawableRes val image: Int) : ImageResource
    data class Vector(val image: ImageVector) : ImageResource

    @Composable
    fun toPainter(): Painter = when (this) {
        is Drawable -> painterResource(id = image)
        is Vector -> rememberVectorPainter(image)
    }
}
