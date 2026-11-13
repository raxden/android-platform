package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.getValueOrNull
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShareResult
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.sync.Mutex

/**
 * Registers an occurrence of [Params.origin] and, when it reaches a milestone accepted by
 * [EngagementEligibility], prompts the user to engage with the app.
 *
 * Every call is counted, even when another engagement flow is already running. Only one flow is
 * shown at a time: the in-app review ([RateAppUseCase]) is tried first and, once the app has
 * been rated, the share prompt ([ShareAppUseCase] with `withPrompt = true`). When the review
 * cannot be launched the share prompt is tried in the same call, so it never stays unreachable.
 * Once both have happened, subsequent calls only count occurrences.
 *
 * A concurrent call made while a flow is in progress is counted and succeeds with
 * [EngagementResult.None] without being queued.
 */
interface EngagementUseCase : SuspendUseCase<EngagementUseCase.Params, EngagementResult> {

    /** @property origin name of the trigger point; occurrences and milestones are tracked per origin. */
    data class Params(val origin: String)
}

internal class EngagementUseCaseImpl(
    private val rateAppUseCase: RateAppUseCase,
    private val shareAppUseCase: ShareAppUseCase,
    private val engagementDataRepository: EngagementDataRepository,
    private val engagementEligibility: EngagementEligibility,
) : EngagementUseCase {

    private val mutex = Mutex()

    override suspend fun invoke(params: EngagementUseCase.Params): Answer<EngagementResult, Failure> =
        track(params.origin).coFlatMap { engagementData -> engageExclusively(engagementData, params.origin) }

    private suspend fun engageExclusively(
        engagementData: EngagementData,
        origin: String,
    ): Answer<EngagementResult, Failure> {
        if (!mutex.tryLock()) return Answer.Success(EngagementResult.None)
        return try {
            engage(engagementData, origin)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun engage(engagementData: EngagementData, origin: String): Answer<EngagementResult, Failure> =
        when {
            !engagementData.isEligible(origin) -> Answer.Success(EngagementResult.None)
            !engagementData.hasRated -> rateOrShare(engagementData)
            !engagementData.hasShared -> share()
            else -> Answer.Success(EngagementResult.None)
        }

    private suspend fun track(origin: String): Answer<EngagementData, Failure> =
        engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
            val incremented = engagementData.increment(origin)
            engagementDataRepository.updateEngagementData(incremented).then { incremented }
        }

    private fun EngagementData.increment(origin: String): EngagementData =
        copy(counts = counts + (origin to (counts[origin] ?: 0) + 1))

    private fun EngagementData.isEligible(origin: String): Boolean =
        engagementEligibility.isEligible(origin, counts[origin] ?: 0)

    private suspend fun rateOrShare(engagementData: EngagementData): Answer<EngagementResult, Failure> =
        rateAppUseCase(RateAppUseCase.Params).coFlatMap { reviewUnavailable ->
            when {
                !reviewUnavailable -> Answer.Success(EngagementResult.ReviewLaunched)
                !engagementData.hasShared -> share().then { it.orReviewUnavailable() }
                else -> Answer.Success(EngagementResult.ReviewUnavailable)
            }
        }

    private fun EngagementResult.orReviewUnavailable(): EngagementResult = when (this) {
        EngagementResult.ShareNotChosen, EngagementResult.None -> EngagementResult.ReviewUnavailable
        else -> this
    }

    private suspend fun share(): Answer<EngagementResult, Failure> =
        when (shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)).getValueOrNull()) {
            is InAppShareResult.TargetChosen -> markAsShared()

            // Declined (prompt rejected) or Dismissed (sheet closed) retry on the next milestone.
            InAppShareResult.Dismissed, InAppShareResult.Declined, null ->
                Answer.Success(EngagementResult.ShareNotChosen)
        }

    private suspend fun markAsShared(): Answer<EngagementResult, Failure> =
        engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
            engagementDataRepository.updateEngagementData(engagementData.copy(hasShared = true))
                .then { EngagementResult.ShareChosen }
        }
}
