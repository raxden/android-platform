package com.raxdenstudios.platform.ui.composable

import android.annotation.SuppressLint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.raxdenstudios.platform.ui.preview.DevicePreviews
import com.raxdenstudios.platform.ui.theme.AppComposeTheme

object Dialog {

    @Composable
    fun Simple(
        title: String,
        message: String,
        isVisible: Boolean = true,
        confirmButton: Pair<String, () -> Unit>,
        dismissButton: Pair<String, () -> Unit>? = null,
        onDismissRequest: () -> Unit = {},
    ) {
        if (!isVisible) return

        AlertDialog(
            modifier = Modifier
                .semantics { contentDescription = title },
            title = { Label.BodyMedium(title) },
            text = { Label.BodyMedium(message) },
            onDismissRequest = onDismissRequest,
            dismissButton = {
                dismissButton?.let {
                    TextButton(
                        content = { Label.BodyMedium(dismissButton.first) },
                        onClick = dismissButton.second,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    content = { Label.BodyMedium(confirmButton.first) },
                    onClick = confirmButton.second,
                )
            },
        )
    }
}


@SuppressLint("VisibleForTests")
@DevicePreviews
@Composable
fun ErrorDialogPreview() {
    AppComposeTheme {
        Dialog.Simple(
            title = "Lorem Ipsum",
            message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
                    "tempor incididunt ut labore et dolore magna aliqua.",
            confirmButton = "ok" to  {}
        )
    }
}
