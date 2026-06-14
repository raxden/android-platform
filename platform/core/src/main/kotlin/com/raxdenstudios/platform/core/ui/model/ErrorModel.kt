package com.raxdenstudios.platform.core.ui.model

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable

@Immutable
data class ErrorModel(
    val title: String,
    val message: String,
) {

    companion object {

        @VisibleForTesting
        val mock = ErrorModel(
            title = "Lorem Ipsum",
            message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit...",
        )
    }
}
