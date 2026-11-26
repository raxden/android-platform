package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.commons.android.compose.preview.DevicePreviews
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.PreviewTheme
import com.raxdenstudios.platform.ui.theme.Spacing

/** Headers placed above a section of a screen. */
object Header {

    /** A [Section] ending with a [Button.TextArrow] that shows [trailingText] and calls [onTrailingClick]. */
    @Composable
    fun Section(
        title: StringResource,
        trailingText: StringResource,
        modifier: Modifier = Modifier,
        onTrailingClick: () -> Unit,
    ) {
        Section(
            modifier = modifier,
            title = title,
            trailing = {
                Button.TextArrow(
                    text = trailingText,
                    onClick = onTrailingClick,
                )
            },
        )
    }

    /** A one-line title after a secondary double arrow, with optional [trailing] content at the end. */
    @Composable
    fun Section(
        title: StringResource,
        modifier: Modifier = Modifier,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.medium,
                    vertical = Spacing.xSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = Icons.DoubleArrow.toPainter(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Label.BodyLarge(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.small),
                maxLines = 1,
                text = title,
            )
            trailing?.invoke()
        }
    }

    /** Primary-coloured [Label.TitleSmall] used as a section subheader. */
    @Composable
    fun Subsection(
        title: StringResource,
        modifier: Modifier = Modifier,
    ) {
        Label.TitleSmall(
            text = title,
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@DevicePreviews
@Composable
internal fun HeaderPreview() {
    PreviewTheme {
        Header.Section(title = StringResource.PlainText("Popular"))
        Header.Section(
            title = StringResource.PlainText("Popular"),
            trailingText = StringResource.PlainText("See all"),
            onTrailingClick = {},
        )
        Header.Subsection(
            title = StringResource.PlainText("Subsection"),
            modifier = Modifier.padding(horizontal = Spacing.large),
        )
    }
}
