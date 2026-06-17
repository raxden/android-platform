package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object BottomSheet {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Simple(
        title: String,
        message: String,
        confirmButton: Pair<String, () -> Unit>,
        dismissButton: Pair<String, () -> Unit>? = null,
        onDismissRequest: () -> Unit = {},
    ) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                Label.TitleMedium(title)
                Spacer(modifier = Modifier.height(8.dp))
                Label.BodyMedium(message)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    dismissButton?.let {
                        TextButton(
                            content = { Label.BodyMedium(it.first) },
                            onClick = it.second,
                        )
                    }
                    TextButton(
                        content = { Label.BodyMedium(confirmButton.first) },
                        onClick = confirmButton.second,
                    )
                }
            }
        }
    }
}
