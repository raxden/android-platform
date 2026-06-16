package com.raxdenstudios.platform.core.util

sealed class IntentType {

    data object AppSettings : IntentType()

    data object PlayStore : IntentType()

    data class ShareText(
        val subject: String,
        val text: String,
    ) : IntentType()
}
