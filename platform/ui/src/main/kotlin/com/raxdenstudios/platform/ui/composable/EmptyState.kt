package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raxdenstudios.commons.android.compose.model.ImageResource
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.commons.android.compose.preview.DevicePreviews
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.PreviewTheme
import com.raxdenstudios.platform.ui.theme.Spacing

/** Centered empty states: illustration, bold title, description and an optional tip. */
object EmptyState {

    /**
     * Standard empty state layout.
     *
     * @param tip optional extra line shown below [description]; omitted when null.
     * @param imageSize side of the square illustration, [EmptyStateDefaults.ImageSize] by default.
     * @param tintVectors tints [ImageResource.Vector] images with the primary colour; drawables always keep their
     * own colours.
     */
    @Composable
    fun Default(
        image: ImageResource,
        title: StringResource,
        description: StringResource,
        modifier: Modifier = Modifier,
        tip: StringResource? = null,
        imageSize: Dp = EmptyStateDefaults.ImageSize,
        tintVectors: Boolean = false,
    ) {
        Column(
            modifier = modifier.padding(horizontal = Spacing.xxLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = image.toPainter(),
                contentDescription = null,
                colorFilter = imageTint(image, tintVectors, MaterialTheme.colorScheme.primary)
                    ?.let { ColorFilter.tint(it) },
                modifier = Modifier.size(imageSize),
            )
            Spacer.Vertical.Large()
            Label.TitleLarge(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                text = title,
                textAlign = TextAlign.Center,
            )
            Spacer.Vertical.Large()
            Label.BodyMedium(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = description,
                textAlign = TextAlign.Center,
            )
            tip?.let {
                Spacer.Vertical.Large()
                Spacer.Vertical.Large()
                Label.BodyMedium(
                    text = it,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Default sizes of [EmptyState]. */
object EmptyStateDefaults {

    /** Side of the square illustration of [EmptyState.Default]. */
    val ImageSize: Dp = 200.dp
}

/** The colour [EmptyState.Default] tints [image] with, or null to draw it with its own colours. */
internal fun imageTint(
    image: ImageResource,
    tintVectors: Boolean,
    tint: Color,
): Color? = when (image) {
    is ImageResource.Vector -> if (tintVectors) tint else null
    is ImageResource.Drawable -> null
}

@DevicePreviews
@Composable
internal fun EmptyStatePreview() {
    PreviewTheme {
        EmptyState.Default(
            image = Icons.LocationOn,
            title = StringResource.PlainText("No results"),
            description = StringResource.PlainText("Try a different search"),
            tip = StringResource.PlainText("Tip: check the spelling"),
            imageSize = 96.dp,
            tintVectors = true,
        )
    }
}
