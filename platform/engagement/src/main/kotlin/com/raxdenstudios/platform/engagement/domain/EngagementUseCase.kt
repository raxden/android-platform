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
 * [EngagementEligibility] (or a previous opportunity is pending), prompts the user to engage with
 * the app.
 *
 * Every call is counted, even when another engagement flow is already running. Only one flow is
 * shown at a time, chosen on each eligible call:
 * 1. the in-app review ([RateAppUseCase]) when the review policy allows it;
 * 2. otherwise the share prompt ([ShareAppUseCase] with `withPrompt = true`) when the share
 *    policy allows it.
 *
 * When the review is allowed but cannot be launched, the share prompt is tried in the same call.
 *
 * Review policy: never once the user explicitly rated the app ([MarkAppAsRatedUseCase]), at most
 * [MAX_REVIEW_ATTEMPTS] launches, at least [REVIEW_COOLDOWN_MILLIS] between launches.
 *
 * Share policy: never once the user picked a share target or declined it ("Not now")
 * [MAX_SHARE_DECLINES] times, at least [SHARE_PROMPT_COOLDOWN_MILLIS] between prompts.
 *
 * Both are also separated by [MIN_PROMPTS_GAP_MILLIS]: no share prompt right after a review
 * launch and no review right after a share prompt, so the user is asked at most once a week.
 *
 * Opportunities are not wasted: when a milestone is reached but nothing can be shown because
 * another flow is in progress, or because neither the review nor the share prompt could launch
 * (e.g. no foreground activity when triggered from a notification action), the prompt is marked
 * as pending ([EngagementData.pendingPrompt]) and the next call is eligible regardless of its
 * milestone. Nothing is marked as pending when the policies allow neither prompt.
 *
 * A concurrent call made while a flow is in progress is counted and succeeds with
 * [EngagementResult.None] without being queued.
 */
interface EngagementUseCase : SuspendUseCase<EngagementUseCase.Params, EngagementResult> {

    /** @property origin name of the trigger point; occurrences and milestones are tracked per origin. */
    data class Params(
        val origin: String,
    )
}

internal class EngagementUseCaseImpl(
    private val rateAppUseCase: RateAppUseCase,
    private val shareAppUseCase: ShareAppUseCase,
    private val engagementDataRepository: EngagementDataRepository,
    private val engagementEligibility: EngagementEligibility,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : EngagementUseCase {

    private val mutex = Mutex()
    private val promptPolicy = PromptPolicy(currentTimeMillis)

    override suspend fun invoke(
        params: EngagementUseCase.Params,
    ): Answer<EngagementResult, Failure> =
        track(params.origin).coFlatMap { engagementData -> engageExclusively(engagementData, params.origin) }

    private suspend fun engageExclusively(
        engagementData: EngagementData,
        origin: String,
    ): Answer<EngagementResult, Failure> {
        if (!mutex.tryLock()) return deferIfEligible(engagementData, origin)
        return try {
            engage(engagementData, origin)
        } finally {
            mutex.unlock()
        }
    }

    /** Another flow holds the screen: keeps a reached milestone for a later call instead of losing it. */
    private suspend fun deferIfEligible(
        engagementData: EngagementData,
        origin: String,
    ): Answer<EngagementResult, Failure> = when {
        engagementData.shouldDefer(origin) -> markPending(EngagementResult.None)
        else -> Answer.Success(EngagementResult.None)
    }

    private fun EngagementData.shouldDefer(
        origin: String,
    ): Boolean = !pendingPrompt && isMilestone(origin) && promptPolicy.canPromptAny(this)

    private suspend fun engage(
        engagementData: EngagementData,
        origin: String,
    ): Answer<EngagementResult, Failure> = when {
        !engagementData.isEligible(origin) -> Answer.Success(EngagementResult.None)

        promptPolicy.canPromptReview(engagementData) -> rateOrShare(engagementData)

        promptPolicy.canPromptShare(engagementData) -> share()

        // A pending prompt that no policy allows any more is dropped rather than kept indefinitely.
        engagementData.pendingPrompt -> clearPending()

        else -> Answer.Success(EngagementResult.None)
    }

    private suspend fun track(
        origin: String,
    ): Answer<EngagementData, Failure> = engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
        val incremented = engagementData.increment(origin)
        engagementDataRepository.updateEngagementData(incremented).then { incremented }
    }

    private fun EngagementData.increment(
        origin: String,
    ): EngagementData = copy(counts = counts + (origin to (counts[origin] ?: 0) + 1))

    private fun EngagementData.isEligible(
        origin: String,
    ): Boolean = pendingPrompt || isMilestone(origin)

    private fun EngagementData.isMilestone(
        origin: String,
    ): Boolean = engagementEligibility.isEligible(origin, counts[origin] ?: 0)

    /** The review clears the pending prompt and records the attempt itself (see [RateAppUseCase]). */
    private suspend fun rateOrShare(
        engagementData: EngagementData,
    ): Answer<EngagementResult, Failure> = rateAppUseCase(RateAppUseCase.Params).coFlatMap { reviewUnavailable ->
        when {
            !reviewUnavailable -> Answer.Success(EngagementResult.ReviewLaunched)
            promptPolicy.canPromptShare(engagementData) -> share().then { it.orReviewUnavailable() }
            else -> markPending(EngagementResult.ReviewUnavailable)
        }
    }

    private fun EngagementResult.orReviewUnavailable(): EngagementResult = when (this) {
        EngagementResult.ShareNotChosen, EngagementResult.None -> EngagementResult.ReviewUnavailable
        else -> this
    }

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

            // The prompt could not be shown: kept pending for the next call.
            null -> markPending(EngagementResult.ShareNotChosen)
        }

    private suspend fun recordSharePrompt(
        result: EngagementResult,
        update: EngagementData.() -> EngagementData,
    ): Answer<EngagementResult, Failure> = updateEngagementData {
        update().copy(lastSharePromptAt = currentTimeMillis(), pendingPrompt = false)
    }.then { result }

    private suspend fun markPending(
        result: EngagementResult,
    ): Answer<EngagementResult, Failure> = updateEngagementData { copy(pendingPrompt = true) }.then { result }

    private suspend fun clearPending(): Answer<EngagementResult, Failure> =
        updateEngagementData { copy(pendingPrompt = false) }.then { EngagementResult.None }

    /** Applies [update] to the latest stored data, which may have changed while a prompt was on screen. */
    private suspend fun updateEngagementData(
        update: EngagementData.() -> EngagementData,
    ): Answer<Unit, Failure> = engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
        engagementDataRepository.updateEngagementData(engagementData.update())
    }

    internal companion object {
        /** "Not now" answers after which the share prompt is never shown again. */
        const val MAX_SHARE_DECLINES = 3

        /** Minimum time between two share prompts. */
        val SHARE_PROMPT_COOLDOWN_MILLIS: Long = TimeUnit.DAYS.toMillis(7)

        /** In-app review launches after which the review is never requested again. */
        const val MAX_REVIEW_ATTEMPTS = 3

        /** Minimum time between two in-app review launches. */
        val REVIEW_COOLDOWN_MILLIS: Long = TimeUnit.DAYS.toMillis(90)

        /** Minimum time between a review launch and a share prompt, in either order. */
        val MIN_PROMPTS_GAP_MILLIS: Long = TimeUnit.DAYS.toMillis(7)
    }
}

/** Review and share rate limits applied by [EngagementUseCaseImpl]; see [EngagementUseCase]. */
private class PromptPolicy(
    private val currentTimeMillis: () -> Long,
) {

    fun canPromptAny(
        engagementData: EngagementData,
    ): Boolean = canPromptReview(engagementData) || canPromptShare(engagementData)

    fun canPromptReview(
        engagementData: EngagementData,
    ): Boolean = with(engagementData) {
        !hasRated && !hasReachedReviewAttemptsLimit() && hasReviewCooldownElapsed() && hasGapSinceSharePromptElapsed()
    }

    fun canPromptShare(
        engagementData: EngagementData,
    ): Boolean = with(engagementData) {
        !hasShared && !hasReachedShareDeclinesLimit() && hasShareCooldownElapsed() && hasGapSinceReviewElapsed()
    }

    private fun EngagementData.hasReachedReviewAttemptsLimit(): Boolean =
        reviewAttempts >= EngagementUseCaseImpl.MAX_REVIEW_ATTEMPTS

    private fun EngagementData.hasReviewCooldownElapsed(): Boolean =
        hasElapsed(lastReviewAt, EngagementUseCaseImpl.REVIEW_COOLDOWN_MILLIS)

    private fun EngagementData.hasGapSinceSharePromptElapsed(): Boolean =
        hasElapsed(lastSharePromptAt, EngagementUseCaseImpl.MIN_PROMPTS_GAP_MILLIS)

    private fun EngagementData.hasReachedShareDeclinesLimit(): Boolean =
        shareDeclines >= EngagementUseCaseImpl.MAX_SHARE_DECLINES

    private fun EngagementData.hasShareCooldownElapsed(): Boolean =
        hasElapsed(lastSharePromptAt, EngagementUseCaseImpl.SHARE_PROMPT_COOLDOWN_MILLIS)

    private fun EngagementData.hasGapSinceReviewElapsed(): Boolean =
        hasElapsed(lastReviewAt, EngagementUseCaseImpl.MIN_PROMPTS_GAP_MILLIS)

    private fun hasElapsed(
        since: Long?,
        interval: Long,
    ): Boolean = since?.let { currentTimeMillis() - it >= interval } ?: true
}
