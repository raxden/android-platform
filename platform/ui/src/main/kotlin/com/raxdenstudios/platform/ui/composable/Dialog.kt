package com.raxdenstudios.platform.ui.composable

import android.annotation.SuppressLint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.raxdenstudios.platform.ui.model.StringResource
import com.raxdenstudios.platform.ui.model.StringResource.PlainText
import com.raxdenstudios.platform.ui.preview.DevicePreviews
import com.raxdenstudios.platform.ui.theme.AppComposeTheme

object Dialog {

    @Composable
    fun Simple(
        title: StringResource,
        message: StringResource,
        isVisible: Boolean = true,
        confirmButton: Pair<StringResource, () -> Unit>,
        dismissButton: Pair<StringResource, () -> Unit>? = null,
        onDismissRequest: () -> Unit,
    ) {
        if (!isVisible) return

        AlertDialog(
            title = { Label.HeadlineSmall(title) },
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
            title = PlainText("Lorem Ipsum"),
            message = PlainText(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
                        "tempor incididunt ut labore et dolore magna aliqua."
            ),
            confirmButton = PlainText("ok") to {},
            onDismissRequest = {},
        )
    }
}
