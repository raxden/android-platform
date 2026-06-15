package com.raxdenstudios.platform.device.data.datasource

import android.content.Context
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.EMPTY
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.model.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FCMLocalDataSource {
    suspend fun saveToken(token: String): Answer<Unit, Failure>
    suspend fun getToken(): Answer<String, Failure>
    suspend fun clearToken(): Answer<Unit, Failure>
}

internal class FCMLocalDataSourceImpl(
    private val context: Context,
) : com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun saveToken(token: String): Answer<Unit, Failure> =
        withContext(Dispatchers.IO) {
            coRunCatching {
                sharedPreferences.edit { putString(KEY_FCM_TOKEN, token) }
            }.thenFailure { error ->
                Failure.Unknown("Failed to save FCM token: ${error.message}")
            }
        }

    override suspend fun getToken(): Answer<String, Failure> =
        withContext(Dispatchers.IO) {
            coRunCatching {
                sharedPreferences.getString(KEY_FCM_TOKEN, String.EMPTY) ?: String.EMPTY
            }.thenFailure { error ->
                Failure.Unknown("Failed to get FCM token: ${error.message}")
            }
        }

    override suspend fun clearToken(): Answer<Unit, Failure> =
        withContext(Dispatchers.IO) {
            coRunCatching {
                sharedPreferences.edit { remove(KEY_FCM_TOKEN) }
            }.thenFailure { error ->
                Failure.Unknown("Failed to clear FCM token: ${error.message}")
            }
        }

    companion object {
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
