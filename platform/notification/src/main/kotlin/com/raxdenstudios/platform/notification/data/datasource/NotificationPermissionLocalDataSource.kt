package com.raxdenstudios.platform.notification.data.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.NotificationStorage

/**
 * Remembers whether the app ever showed the system notification dialog. Android does not tell a permission never
 * asked from one permanently denied (both stop showing the rationale), so this flag tells them apart.
 */
internal interface NotificationPermissionLocalDataSource {

    suspend fun wasRequested(): Answer<Boolean, Failure>

    suspend fun markAsRequested(): Answer<Unit, Failure>
}

internal class NotificationPermissionLocalDataSourceImpl(
    private val sharedPreferences: SharedPreferences,
) : NotificationPermissionLocalDataSource {

    override suspend fun wasRequested(): Answer<Boolean, Failure> = coRunCatching {
        sharedPreferences.getBoolean(NotificationStorage.PERMISSION_REQUESTED_KEY, false)
    }.thenFailure { error -> error.toUnknownError("Error reading the notification permission request") }

    override suspend fun markAsRequested(): Answer<Unit, Failure> = coRunCatching {
        sharedPreferences.edit { putBoolean(NotificationStorage.PERMISSION_REQUESTED_KEY, true) }
    }.thenFailure { error -> error.toUnknownError("Error saving the notification permission request") }
}
