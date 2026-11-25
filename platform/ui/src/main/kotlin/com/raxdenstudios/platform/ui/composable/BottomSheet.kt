package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.platform.ui.theme.Spacing.xLarge

object BottomSheet {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Simple(
        onDismissRequest: () -> Unit = {},
        sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title: StringResource,
        message: StringResource,
        primaryButton: Pair<StringResource, () -> Unit>,
        secondaryButton: Pair<StringResource, () -> Unit>? = null,
    ) {
        Simple(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            primaryButton = primaryButton,
            secondaryButton = secondaryButton,
        ) {
            Label.TitleLarge(title)
            Spacer.Vertical.Small()
            Label.BodyMedium(message)
            Spacer.Vertical.Custom(xLarge)
        }
    }

    /**
     * Modal bottom sheet with a free [content] column and an optional row of text buttons at the end.
     *
     * @param primaryButton label and action of the trailing button; `null` hides it.
     * @param secondaryButton label and action of the button before [primaryButton]; `null` hides it.
     * @param horizontalPadding side margin of the content and the button row. Pass `0.dp` when the
     * content brings its own margins (e.g. full-width search fields or cards) to avoid a double inset.
     * @param content body of the sheet, laid out in a [Column].
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Simple(
        onDismissRequest: () -> Unit = {},
        sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        primaryButton: Pair<StringResource, () -> Unit>? = null,
        secondaryButton: Pair<StringResource, () -> Unit>? = null,
        horizontalPadding: Dp = xLarge,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = xLarge),
            ) {
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    secondaryButton?.let {
                        TextButton(
                            content = { Label.BodyMedium(it.first) },
                            onClick = it.second,
                        )
                    }
                    primaryButton?.let {
                        TextButton(
                            content = { Label.BodyMedium(it.first) },
                            onClick = it.second,
                        )
                    }
                }
            }
        }
    }
}
