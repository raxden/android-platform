package com.raxdenstudios.platform.rating.domain

import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.platform.rating.data.RatingRepository
import com.raxdenstudios.platform.rating.domain.model.RatingData
import java.util.concurrent.TimeUnit

interface ShouldShowRatingDialogUseCase {
    suspend operator fun invoke(): Answer<Boolean, Failure>
}

class ShouldShowRatingDialogUseCaseImpl(
    private val ratingRepository: RatingRepository,
) : ShouldShowRatingDialogUseCase {

    override suspend fun invoke(): Answer<Boolean, Failure> =
        ratingRepository.getRatingData()
            .then { ratingState -> shouldShowRating(ratingState) }

    private fun shouldShowRating(ratingData: RatingData): Boolean {
        if (ratingData.hasRated) return false

        val currentTimeMs = System.currentTimeMillis()
        val daysSinceInstall = TimeUnit.MILLISECONDS.toDays(
            currentTimeMs - ratingData.firstInstallTimeMs
        )

        val hasEnoughTimeSinceInstall = daysSinceInstall >= DAYS_UNTIL_PROMPT
        val hasNotExceededMaxDismissals = ratingData.dismissCount < MAX_DISMISS_COUNT
        val hasEnoughTimeSinceLastDismiss = ratingData.dismissCount == 0 || run {
            val daysSinceLastDismiss = TimeUnit.MILLISECONDS.toDays(
                currentTimeMs - ratingData.lastDismissTimeMs
            )
            daysSinceLastDismiss >= DAYS_BETWEEN_PROMPTS
        }

        return hasEnoughTimeSinceInstall && hasNotExceededMaxDismissals && hasEnoughTimeSinceLastDismiss
    }

    companion object {
        private const val DAYS_UNTIL_PROMPT = 3L
        private const val DAYS_BETWEEN_PROMPTS = 7L
        private const val MAX_DISMISS_COUNT = 3
    }
}
