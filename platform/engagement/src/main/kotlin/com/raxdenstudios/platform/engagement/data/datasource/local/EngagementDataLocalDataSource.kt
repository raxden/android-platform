package com.raxdenstudios.platform.engagement.data.datasource.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.ext.toUnknownError
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface EngagementDataLocalDataSource {

    fun observeEngagementData(): Flow<Answer<EngagementData, Failure>>

    suspend fun getEngagementData(): Answer<EngagementData, Failure>

    suspend fun updateEngagementData(engagementData: EngagementData): Answer<Unit, Failure>
}

class EngagementDataLocalDataSourceImpl(private val json: Json, private val sharedPreferences: SharedPreferences) :
    EngagementDataLocalDataSource {

    private val _engagementData = MutableStateFlow(load())

    override fun observeEngagementData(): Flow<Answer<EngagementData, Failure>> =
        _engagementData.map { Answer.Success(it) }

    override suspend fun getEngagementData(): Answer<EngagementData, Failure> = coRunCatching { load() }
        .thenFailure { error ->
            error.toUnknownError("Error to get engagement data")
        }

    override suspend fun updateEngagementData(engagementData: EngagementData): Answer<Unit, Failure> = coRunCatching {
        sharedPreferences.persist(engagementData)
        _engagementData.value = engagementData
    }.thenFailure { error ->
        error.toUnknownError("Error to update engagement data")
    }

    private fun load(): EngagementData = sharedPreferences.getString(ENGAGEMENT_DATA_KEY, null)?.let {
        runCatching { json.decodeFromString<EngagementData>(it) }
            .getOrElse { EngagementData() }
    } ?: EngagementData()

    private fun SharedPreferences.persist(engagementData: EngagementData) {
        edit { putString(ENGAGEMENT_DATA_KEY, json.encodeToString<EngagementData>(engagementData)) }
    }

    companion object {
        private const val ENGAGEMENT_DATA_KEY = "engagement_data"
    }
}
