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
import com.raxdenstudios.platform.ui.model.StringResource
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
            Label.TitleLarge(title.toText())
            Spacer.Vertical.Small()
            Label.BodyMedium(message.toText())
            Spacer.Vertical.Custom(xLarge)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Simple(
        onDismissRequest: () -> Unit = {},
        sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        primaryButton: Pair<StringResource, () -> Unit>? = null,
        secondaryButton: Pair<StringResource, () -> Unit>? = null,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = xLarge)
                    .padding(bottom = xLarge),
            ) {
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    secondaryButton?.let {
                        TextButton(
                            content = { Label.BodyMedium(it.first.toText()) },
                            onClick = it.second,
                        )
                    }
                    primaryButton?.let {
                        TextButton(
                            content = { Label.BodyMedium(it.first.toText()) },
                            onClick = it.second,
                        )
                    }
                }
            }
        }
    }
}
