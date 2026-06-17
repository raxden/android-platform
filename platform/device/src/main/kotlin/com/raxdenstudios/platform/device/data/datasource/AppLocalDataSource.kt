package com.raxdenstudios.platform.device.data.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.model.Failure
import kotlinx.coroutines.flow.MutableStateFlow

interface AppAccessLocalDataSource {
    suspend fun getLastAccess(): Answer<Long, Failure>
    suspend fun saveLastAccess(timestamp: Long): Answer<Unit, Failure>
}

internal class AppAccessLocalDataSourceImpl(
    private val sharedPreferences: SharedPreferences,
) : AppAccessLocalDataSource {

    private val _lastAccess = MutableStateFlow(load())

    override suspend fun getLastAccess(): Answer<Long, Failure> =
        coRunCatching { load() }
            .thenFailure { error ->
                error.toUnknownError("Error to get last access")
            }

    override suspend fun saveLastAccess(timestamp: Long): Answer<Unit, Failure> =
        coRunCatching {
            sharedPreferences.persist(timestamp)
            _lastAccess.value = timestamp
        }.thenFailure { error ->
            error.toUnknownError("Error to save last access")
        }

    private fun load(): Long =
        sharedPreferences.getLong(LAST_ACCESS_KEY, System.currentTimeMillis())

    private fun SharedPreferences.persist(timestamp: Long) {
        edit { putLong(LAST_ACCESS_KEY, timestamp) }
    }

    private fun Throwable.toUnknownError(errorMessage: String): Failure.Unknown =
        Failure.Unknown("$errorMessage: $message")

    companion object {
        private const val LAST_ACCESS_KEY = "last_access"
    }
}