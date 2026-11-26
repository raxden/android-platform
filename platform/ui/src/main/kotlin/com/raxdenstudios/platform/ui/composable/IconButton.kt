package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raxdenstudios.commons.android.compose.model.ImageResource
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.commons.android.compose.preview.DevicePreviews
import com.raxdenstudios.platform.ui.R
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.PreviewTheme
import com.raxdenstudios.platform.ui.theme.Spacing

/**
 * Icon-only buttons tinted with the primary colour. They shrink while pressed and tap the haptics when
 * [LocalPressFeedback] enables it.
 */
object IconButton {

    private val BackCircleSize = 36.dp

    /** An icon button for any action; [contentDescription] is read by TalkBack. */
    @Composable
    fun Default(
        icon: ImageResource,
        contentDescription: StringResource,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        contentColor: Color = MaterialTheme.colorScheme.primary,
        iconSize: Dp? = null,
        onClick: () -> Unit,
    ) {
        FeedbackIconButton(
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors().copy(contentColor = contentColor),
            iconSize = iconSize,
            onClick = onClick,
        )
    }

    /** Navigates back; [icon] is the small [Icons.BackArrow] chevron unless another is given. */
    @Composable
    fun Back(
        modifier: Modifier = Modifier,
        icon: ImageResource = Icons.BackArrow,
        enabled: Boolean = true,
        contentColor: Color = MaterialTheme.colorScheme.primary,
        onClick: () -> Unit,
    ) {
        Default(
            icon = icon,
            contentDescription = StringResource.ResourceId(R.string.platform_ui_back),
            modifier = modifier,
            enabled = enabled,
            contentColor = contentColor,
            onClick = onClick,
        )
    }

    /** Closes the current surface with the [Icons.Close] cross, read as "Close" by TalkBack. */
    @Composable
    fun Close(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        contentColor: Color = MaterialTheme.colorScheme.primary,
        onClick: () -> Unit,
    ) {
        Default(
            icon = Icons.Close,
            contentDescription = StringResource.ResourceId(R.string.platform_ui_close),
            modifier = modifier,
            enabled = enabled,
            contentColor = contentColor,
            onClick = onClick,
        )
    }

    /** A 36dp primary circle with the back chevron, for screens drawn under the top bar (a hero image). */
    @Composable
    fun BackCircle(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        FeedbackIconButton(
            icon = Icons.BackArrow,
            contentDescription = StringResource.ResourceId(R.string.platform_ui_back),
            modifier = modifier
                .padding(Spacing.small)
                .size(BackCircleSize),
            colors = IconButtonDefaults.iconButtonColors().copy(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            enabled = enabled,
            onClick = onClick,
        )
    }

    @Composable
    private fun FeedbackIconButton(
        icon: ImageResource,
        contentDescription: StringResource,
        modifier: Modifier,
        colors: IconButtonColors,
        onClick: () -> Unit,
        enabled: Boolean = true,
        iconSize: Dp? = null,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        IconButton(
            modifier = modifier.pressFeedbackScale(
                interactionSource = interactionSource,
                pressedScale = LocalPressFeedback.current.iconButtonPressedScale,
            ),
            onClick = rememberPressFeedbackClick(onClick),
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        ) {
            Icon(
                modifier = iconSize?.let { Modifier.size(iconSize) } ?: Modifier,
                painter = icon.toPainter(),
                contentDescription = contentDescription.toText(),
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun IconButtonsPreview() {
    PreviewTheme {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            IconButton.Back(onClick = {})
            IconButton.Back(icon = Icons.ArrowBack, onClick = {})
            IconButton.Close(onClick = {})
            IconButton.BackCircle(onClick = {})
            IconButton.Default(
                icon = Icons.LocationOn,
                contentDescription = StringResource.PlainText("Location"),
                onClick = {},
            )
            IconButton.Default(
                icon = Icons.LocationOn,
                contentDescription = StringResource.PlainText("Location"),
                enabled = false,
                onClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun IconButtonsPressFeedbackPreview() {
    PreviewTheme {
        CompositionLocalProvider(
            LocalPressFeedback provides PressFeedback(iconButtonPressedScale = 0.88f, hapticsEnabled = true),
        ) {
            Column(modifier = Modifier.padding(Spacing.large)) {
                IconButton.Back(icon = Icons.ArrowBack, onClick = {})
                IconButton.Close(onClick = {})
            }
        }
    }
}
