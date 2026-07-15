package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raxdenstudios.commons.core.ext.EMPTY
import com.raxdenstudios.platform.ui.preview.DevicePreviews
import com.raxdenstudios.platform.ui.theme.AppComposeTheme
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.Spacing

object Item {

    object Card {

        @Composable
        private fun Container(
            modifier: Modifier = Modifier,
            selected: Boolean = false,
            onClick: () -> Unit = {},
            content: @Composable ColumnScope.() -> Unit,
        ) {
            val containerColor = when (selected) {
                true -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large),
                border = BorderStroke(0.5.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MinHeight)
                        .clickable { onClick() },
                    verticalArrangement = Arrangement.Center,
                ) {
                    content()
                }
            }
        }

        @Composable
        fun Default(
            modifier: Modifier = Modifier,
            leadingContent: @Composable (RowScope.() -> Unit)? = null,
            trailingContent: @Composable (RowScope.() -> Unit)? = null,
            bottomContent: @Composable (ColumnScope.() -> Unit)? = null,
            selected: Boolean = false,
            onClick: () -> Unit = {},
            content: @Composable ColumnScope.() -> Unit,
        ) {
            Container(
                modifier = modifier,
                selected = selected,
                onClick = onClick,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Spacing.large,
                            vertical = Spacing.medium
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingContent != null) {
                        leadingContent()
                        Spacer.Horizontal.Small()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        content()
                    }
                    if (trailingContent != null) {
                        Spacer.Horizontal.Small()
                        trailingContent()
                    }
                }
                if (bottomContent != null) {
                    bottomContent()
                }
            }
        }

        @Composable
        fun Default(
            modifier: Modifier = Modifier,
            title: String,
            description: String = String.EMPTY,
            leadingContent: @Composable (RowScope.() -> Unit)? = null,
            trailingContent: @Composable (RowScope.() -> Unit)? = null,
            bottomContent: @Composable (ColumnScope.() -> Unit)? = null,
            selected: Boolean = false,
            onClick: () -> Unit = {},
        ) {
            Default(
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                bottomContent = bottomContent,
                selected = selected,
                onClick = onClick
            ) {
                Label.BodyMedium(text = title)
                if (description.isNotEmpty()) {
                    Label.BodySmall(text = description)
                }
            }
        }

        @Composable
        fun Navigation(
            modifier: Modifier = Modifier,
            title: String,
            description: String = String.EMPTY,
            selected: Boolean = false,
            onClick: () -> Unit = {},
        ) {
            Default(
                modifier = modifier,
                title = title,
                description = description,
                selected = selected,
                trailingContent = {
                    Icon(
                        painter = Icons.ChevronRight.toPainter(),
                        contentDescription = "back",
                    )
                },
                onClick = onClick
            )
        }

        @Composable
        fun Switch(
            modifier: Modifier = Modifier,
            title: String,
            description: String = String.EMPTY,
            checked: Boolean = false,
            onCheckedChange: (Boolean) -> Unit = {},
        ) {
            Default(
                modifier = modifier,
                title = title,
                description = description,
                trailingContent = { Switch(checked, onCheckedChange) },
            )
        }

        @Composable
        fun RadioButton(
            modifier: Modifier = Modifier,
            title: String,
            description: String = String.EMPTY,
            selected: Boolean = false,
            onClick: () -> Unit = {},
        ) {
            RadioButton(
                modifier = modifier,
                selected = selected,
                onClick = onClick,
            ) {
                Label.BodyMedium(text = title)
                if (description.isNotEmpty()) {
                    Label.BodySmall(text = description)
                }
            }
        }

        @Composable
        fun RadioButton(
            modifier: Modifier = Modifier,
            selected: Boolean = false,
            onClick: () -> Unit = {},
            content: @Composable ColumnScope.() -> Unit,
        ) {
            Default(
                modifier = modifier,
                content = content,
                trailingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                },
                onClick = onClick,
            )
        }

        @Composable
        fun Checkbox(
            modifier: Modifier = Modifier,
            title: String,
            description: String = String.EMPTY,
            checked: Boolean = false,
            onCheckedChange: (Boolean) -> Unit = {},
        ) {
            Checkbox(
                modifier = modifier,
                checked = checked,
                onCheckedChange = onCheckedChange,
            ) {
                Label.BodyMedium(text = title)
                if (description.isNotEmpty()) {
                    Label.BodySmall(text = description)
                }
            }
        }

        @Composable
        fun Checkbox(
            modifier: Modifier = Modifier,
            checked: Boolean = false,
            leadingContent: @Composable (RowScope.() -> Unit)? = null,
            onCheckedChange: (Boolean) -> Unit = {},
            content: @Composable ColumnScope.() -> Unit,
        ) {
            Default(
                modifier = modifier,
                content = content,
                onClick = { onCheckedChange(!checked) },
                leadingContent = leadingContent,
                trailingContent = {
                    Checkbox(
                        modifier = Modifier.size(20.dp),
                        checked = checked,
                        onCheckedChange = null
                    )
                },
            )
        }
    }
}

@DevicePreviews
@Composable
fun ListItemsPreview() {
    AppComposeTheme {
        Column {
            Item.Card.Default(
                leadingContent = {
                    Icon(
                        painter = Icons.LocationOn.toPainter(),
                        contentDescription = "Location Icon",
                    )
                },
                title = "Item Text",
                description = "Item Text description",
            ) {}
            Item.Card.Navigation(title = "Item Text") {}
            Item.Card.Switch(
                title = "Item Text",
                description = "Item Description Item Description Item Description Item Description" +
                        " Item Description Item Description"
            ) {}
            Item.Card.RadioButton(
                title = "Item Text",
                description = "Item Description Item Description Item Description Item Description" +
                        " Item Description Item Description"
            ) {}
            Item.Card.RadioButton(
                title = "Item Text",
            ) {}
            Item.Card.Checkbox(
                title = "Item Text",
                description = "Item Description Item Description Item Description Item Description" +
                        " Item Description Item Description"
            ) {}
        }
    }
}
