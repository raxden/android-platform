package com.raxdenstudios.component.rating.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.commons.coroutines.ext.coThen
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.component.rating.data.RatingRepository

interface UpdateRatingStateUseCase {
    suspend fun markAsRated(): Answer<Unit, Failure>
    suspend fun markAsDismissed(): Answer<Unit, Failure>
}

internal class UpdateRatingStateUseCaseImpl(
    private val ratingRepository: RatingRepository,
) : UpdateRatingStateUseCase {

    override suspend fun markAsRated(): Answer<Unit, Failure> =
        ratingRepository.getRatingData()
            .then { ratingData -> ratingData.copy(hasRated = true) }
            .coThen { ratingData -> ratingRepository.updateRatingData(ratingData) }

    override suspend fun markAsDismissed(): Answer<Unit, Failure> =
        ratingRepository.getRatingData()
            .then { ratingData ->
                ratingData.copy(
                    dismissCount = ratingData.dismissCount + 1,
                    lastDismissTimeMs = System.currentTimeMillis()
                )
            }
            .coThen { ratingData -> ratingRepository.updateRatingData(ratingData) }
}
