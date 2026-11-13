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
 * Prompts the user to engage with the app from the given [Params.origin]. Only one engagement
 * flow is ever shown at a time: the in-app review ([RateAppUseCase]) is tried first and, once the
 * app has been rated, the share prompt ([ShareAppUseCase] with `withPrompt = true`) is tried
 * next. Once both have happened, subsequent calls are a no-op.
 *
 * The use case succeeds with `true` when the caller should navigate to the Play Store rating page
 * instead: the review flow could not be launched and the app has not been rated yet.
 *
 * A [Mutex] guarantees a single engagement flow is launched at a time: a concurrent call made
 * while one is already in progress succeeds with `false` without being queued.
 */
interface EngagementUseCase : SuspendUseCase<EngagementUseCase.Params, Boolean> {

    data class Params(val origin: String)
}

class EngagementUseCaseImpl(
    private val rateAppUseCase: RateAppUseCase,
    private val shareAppUseCase: ShareAppUseCase,
    private val engagementDataRepository: EngagementDataRepository,
    private val engagementEligibility: EngagementEligibility,
) : EngagementUseCase {

    private val mutex = Mutex()

    override suspend fun invoke(params: EngagementUseCase.Params): Answer<Boolean, Failure> {
        if (!mutex.tryLock()) return Answer.Success(false)
        return try {
            track(params.origin).coFlatMap { engagementData ->
                engage(
                    engagementData,
                    params.origin,
                )
            }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun engage(engagementData: EngagementData, origin: String): Answer<Boolean, Failure> = when {
        !engagementData.isEligible(origin) -> Answer.Success(false)
        !engagementData.hasRated -> rate()
        !engagementData.hasShared -> share(engagementData)
        else -> Answer.Success(false)
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

    private suspend fun rate(): Answer<Boolean, Failure> = rateAppUseCase(RateAppUseCase.Params)

    private suspend fun share(engagementData: EngagementData): Answer<Boolean, Failure> =
        when (shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)).getValueOrNull()) {
            is InAppShareResult.TargetChosen -> markAsShared(engagementData)

            // Declined (prompt rejected) or Dismissed (sheet closed) retry on the next milestone.
            InAppShareResult.Dismissed, InAppShareResult.Declined, null -> Answer.Success(false)
        }

    private suspend fun markAsShared(engagementData: EngagementData): Answer<Boolean, Failure> =
        engagementDataRepository.updateEngagementData(engagementData.copy(hasShared = true))
            .then { false }
}
