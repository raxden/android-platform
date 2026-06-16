package com.raxdenstudios.platform.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.raxdenstudios.commons.core.ext.EMPTY

sealed interface StringResource {
    data class ResourceId(
        @param:StringRes val resourceId: Int,
        val params: List<String> = emptyList(),
    ) : StringResource

    data class PlainText(val value: String) : StringResource

    data object Empty : StringResource

    @Composable
    @Suppress("SpreadOperator")
    fun toText(): String = when (this) {
        is ResourceId -> {
            if (params.isEmpty()) {
                stringResource(id = resourceId)
            } else {
                stringResource(id = resourceId, *params.toTypedArray())
            }
        }

        is PlainText -> value
        is Empty -> String.EMPTY
    }
}
