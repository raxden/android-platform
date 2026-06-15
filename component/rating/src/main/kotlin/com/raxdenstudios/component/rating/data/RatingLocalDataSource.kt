package com.raxdenstudios.component.rating.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.thenFailure
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.component.rating.domain.model.RatingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface RatingLocalDataSource {

    fun observeRatingData(): Flow<Answer<RatingData, Failure>>

    suspend fun getRatingData(): Answer<RatingData, Failure>

    suspend fun updateRatingData(ratingData: RatingData): Answer<Unit, Failure>
}

internal class RatingLocalDataSourceImpl(
    private val json: Json,
    private val sharedPreferences: SharedPreferences,
) : RatingLocalDataSource {

    private val _ratingState = MutableStateFlow(load())

    override fun observeRatingData(): Flow<Answer<RatingData, Failure>> =
        _ratingState.map { Answer.Success(it) }

    override suspend fun getRatingData(): Answer<RatingData, Failure> =
        coRunCatching { load() }
            .thenFailure { error ->
                error.toUnknownError("Error to get Rating")
            }

    override suspend fun updateRatingData(
        ratingData: RatingData,
    ): Answer<Unit, Failure> =
        coRunCatching {
            sharedPreferences.persist(ratingData)
            _ratingState.value = ratingData
        }.thenFailure { error ->
            error.toUnknownError("Error to update ratingState")
        }

    private fun load(): RatingData {
        return sharedPreferences.load(defaultRating = RatingData())
    }

    private fun SharedPreferences.load(defaultRating: RatingData): RatingData =
        getString(RATING_STATE_KEY, null)?.let {
            runCatching { json.decodeFromString<RatingData>(it) }
                .getOrElse { defaultRating }
        } ?: defaultRating

    private fun SharedPreferences.persist(ratingData: RatingData) {
        edit { putString(RATING_STATE_KEY, json.encodeToString<RatingData>(ratingData)) }
    }

    private fun Throwable.toUnknownError(errorMessage: String): Failure.Unknown =
        Failure.Unknown("$errorMessage: $message")

    companion object {
        private const val RATING_STATE_KEY = "ratingState"
    }
}
