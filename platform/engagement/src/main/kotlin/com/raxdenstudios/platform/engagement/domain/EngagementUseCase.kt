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
import java.util.concurrent.TimeUnit

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
 * The share prompt is rate-limited: after being shown it stays silent for
 * [SHARE_PROMPT_COOLDOWN_MILLIS] even if further milestones are reached, and once the user
 * declines it ("Not now") [MAX_SHARE_DECLINES] times it is never shown again.
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
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
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
            engagementData.canPromptShare() -> share()
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
                engagementData.canPromptShare() -> share().then { it.orReviewUnavailable() }
                else -> Answer.Success(EngagementResult.ReviewUnavailable)
            }
        }

    private fun EngagementResult.orReviewUnavailable(): EngagementResult = when (this) {
        EngagementResult.ShareNotChosen, EngagementResult.None -> EngagementResult.ReviewUnavailable
        else -> this
    }

    private fun EngagementData.canPromptShare(): Boolean =
        !hasShared && !hasReachedShareDeclinesLimit() && hasShareCooldownElapsed()

    private fun EngagementData.hasReachedShareDeclinesLimit(): Boolean = shareDeclines >= MAX_SHARE_DECLINES

    private fun EngagementData.hasShareCooldownElapsed(): Boolean =
        lastSharePromptAt?.let { currentTimeMillis() - it >= SHARE_PROMPT_COOLDOWN_MILLIS } ?: true

    private suspend fun share(): Answer<EngagementResult, Failure> =
        when (shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)).getValueOrNull()) {
            is InAppShareResult.TargetChosen -> recordSharePrompt(EngagementResult.ShareChosen) {
                copy(hasShared = true)
            }

            // "Not now": counts towards the declines cap and waits for the cooldown.
            InAppShareResult.Declined -> recordSharePrompt(EngagementResult.ShareNotChosen) {
                copy(shareDeclines = shareDeclines + 1)
            }

            // Prompt accepted but sheet closed: waits for the cooldown without counting as a decline.
            InAppShareResult.Dismissed -> recordSharePrompt(EngagementResult.ShareNotChosen) { this }

            // The prompt could not be shown: retried on the next milestone.
            null -> Answer.Success(EngagementResult.ShareNotChosen)
        }

    private suspend fun recordSharePrompt(
        result: EngagementResult,
        update: EngagementData.() -> EngagementData,
    ): Answer<EngagementResult, Failure> = engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
        val updated = engagementData.update().copy(lastSharePromptAt = currentTimeMillis())
        engagementDataRepository.updateEngagementData(updated).then { result }
    }

    internal companion object {
        /** "Not now" answers after which the share prompt is never shown again. */
        const val MAX_SHARE_DECLINES = 3

        /** Minimum time between two share prompts. */
        val SHARE_PROMPT_COOLDOWN_MILLIS: Long = TimeUnit.DAYS.toMillis(7)
    }
}
