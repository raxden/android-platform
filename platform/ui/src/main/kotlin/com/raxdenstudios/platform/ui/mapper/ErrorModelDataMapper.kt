package com.raxdenstudios.platform.ui.mapper

import com.raxdenstudios.platform.ui.model.ErrorModel
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.platform.ui.R
import com.raxdenstudios.platform.core.domain.model.Failure

interface ErrorModelDataMapper {
    fun toErrorModel(source: Failure): ErrorModel
}

class ErrorModelDataMapperImpl(
    private val stringProvider: StringProvider,
) : ErrorModelDataMapper {

    override fun toErrorModel(source: Failure): ErrorModel = when (source) {
        is Failure.Network.Client -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = source.message,
        )

        is Failure.Network.Connection -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = stringProvider.getString(R.string.error_lost_connection),
        )

        is Failure.Network.Server -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = source.message,
        )

        is Failure.Network.Unknown -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = stringProvider.getString(R.string.error_unexpected_error_message),
        )

        is Failure.ResourceNotFound -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = stringProvider.getString(R.string.error_unexpected_error_message),
        )

        is Failure.Unauthorized -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = stringProvider.getString(R.string.error_unexpected_error_message),
        )

        is Failure.Unknown -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = stringProvider.getString(R.string.error_unexpected_error_message),
        )

        else -> ErrorModel(
            title = stringProvider.getString(R.string.error_information),
            message = stringProvider.getString(R.string.error_unexpected_error_message),
        )
    }
}
