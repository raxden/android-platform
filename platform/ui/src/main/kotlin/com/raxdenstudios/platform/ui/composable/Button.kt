package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raxdenstudios.commons.android.compose.model.ImageResource
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.commons.android.compose.preview.DevicePreviews
import com.raxdenstudios.platform.ui.motion.appearShine
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.PreviewTheme
import com.raxdenstudios.platform.ui.theme.Spacing
import androidx.compose.material3.Card as MaterialCard

/**
 * Text buttons. [Default], [Transparent], [Primary] and [Secondary] shrink while pressed and tap the haptics when
 * [LocalPressFeedback] enables it; `modifier` always applies to the button, never to its label.
 */
object Button {

    private val LargeButtonHeight = 56.dp
    private val CardIconSize = 28.dp
    private val CardBorderWidth = 0.5.dp
    private const val CARD_BORDER_ALPHA = 0.12f

    /** A filled Material button labelled with [text]; [colors] restyles its container and content. */
    @Composable
    fun Default(
        text: StringResource,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Button(
            modifier = modifier.buttonPressScale(interactionSource),
            colors = colors,
            onClick = rememberPressFeedbackClick(onClick),
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            ButtonText(text)
        }
    }

    /** A [Default] button with no container nor elevation and primary text. */
    @Composable
    fun Transparent(
        text: StringResource,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Button(
            modifier = modifier.buttonPressScale(interactionSource),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
            onClick = rememberPressFeedbackClick(onClick),
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            ButtonText(text)
        }
    }

    /**
     * Large, full-width main action of a screen with bold text. With [appearShine] a soft light sweeps across it
     * shortly after it appears (skipped with reduced motion).
     */
    @Composable
    fun Primary(
        text: StringResource,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        appearShine: Boolean = true,
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val shape = ButtonDefaults.shape
        Button(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = LargeButtonHeight)
                .buttonPressScale(interactionSource)
                .then(if (appearShine) Modifier.appearShine(shape) else Modifier),
            shape = shape,
            onClick = rememberPressFeedbackClick(onClick),
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            ButtonText(text, FontWeight.Bold)
        }
    }

    /** Full-width outlined action placed next to a [Primary] one. */
    @Composable
    fun Secondary(
        text: StringResource,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        OutlinedButton(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = LargeButtonHeight)
                .buttonPressScale(interactionSource),
            onClick = rememberPressFeedbackClick(onClick),
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            ButtonText(text)
        }
    }

    /**
     * A selectable card with a primary icon over a label, such as one option of a theme picker. TalkBack reads it as
     * a radio button with its selected state; it ignores [LocalPressFeedback]. With [enabled] false it keeps its
     * colours and stops reacting to clicks.
     */
    @Composable
    fun Card(
        icon: ImageResource,
        label: StringResource,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    ) {
        val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = CARD_BORDER_ALPHA)
        val containerColor = when (selected) {
            true -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        }

        MaterialCard(
            modifier = modifier.selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
            border = BorderStroke(CardBorderWidth, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = Spacing.large)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Icon(
                    painter = icon.toPainter(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(CardIconSize),
                )
                Label.BodyMedium(text = label)
            }
        }
    }

    /** A clickable label followed by a right arrow, such as a "See all" link; it ignores [LocalPressFeedback]. */
    @Composable
    fun TextArrow(
        text: StringResource,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = modifier.clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label.LabelMedium(text)
            Icon(
                painter = Icons.KeyboardArrowRight.toPainter(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }

    @Composable
    private fun Modifier.buttonPressScale(
        interactionSource: MutableInteractionSource,
    ): Modifier = pressFeedbackScale(
        interactionSource = interactionSource,
        pressedScale = LocalPressFeedback.current.buttonPressedScale,
    )

    @Composable
    private fun ButtonText(
        text: StringResource,
        fontWeight: FontWeight? = null,
    ) {
        Label.TitleLarge(
            modifier = Modifier.padding(Spacing.xSmall),
            text = text,
            textAlign = TextAlign.Center,
            fontWeight = fontWeight,
        )
    }
}

@DevicePreviews
@Composable
internal fun ButtonsPreview() {
    PreviewTheme {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button.Card(
                    modifier = Modifier.weight(1f),
                    icon = Icons.LocationOn,
                    label = StringResource.PlainText("Selected"),
                    selected = true,
                    onClick = {},
                )
                Button.Card(
                    modifier = Modifier.weight(1f),
                    icon = Icons.LocationOn,
                    label = StringResource.PlainText("Not selected"),
                    selected = false,
                    onClick = {},
                )
            }
            Button.TextArrow(text = StringResource.PlainText("SEE ALL"), onClick = {})
            Button.Default(text = StringResource.PlainText("Confirm"), onClick = {})
            Button.Default(text = StringResource.PlainText("Disabled"), enabled = false, onClick = {})
            Button.Transparent(text = StringResource.PlainText("Transparent"), onClick = {})
        }
    }
}

@DevicePreviews
@Composable
internal fun LargeButtonsPreview() {
    PreviewTheme {
        CompositionLocalProvider(
            LocalPressFeedback provides PressFeedback(buttonPressedScale = 0.96f, hapticsEnabled = true),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Button.Primary(text = StringResource.PlainText("Continue"), onClick = {})
                Button.Secondary(text = StringResource.PlainText("New game"), onClick = {})
                Button.Primary(text = StringResource.PlainText("Disabled"), enabled = false, onClick = {})
            }
        }
    }
}
