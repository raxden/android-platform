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
import com.raxdenstudios.platform.engagement.domain.model.EngagementPolicy
import com.raxdenstudios.platform.engagement.domain.model.ShareState
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration

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
 * Every limit comes from [EngagementPolicy] (defaults in brackets):
 * - Install age: nothing is shown until [EngagementPolicy.minInstallAge] (3 days) has passed
 *   since the app was first installed; milestones reached earlier are counted but neither shown
 *   nor kept pending.
 * - Review: never once the user explicitly rated the app ([MarkAppAsRatedUseCase]), at most
 *   [EngagementPolicy.maxReviewAttempts] (3) launches, at least [EngagementPolicy.reviewCooldown]
 *   (90 days) between launches.
 * - Share: never once the user picked a share target or declined it
 *   [EngagementPolicy.maxShareDeclines] (2) times, at least [EngagementPolicy.shareCooldown]
 *   (30 days) between prompts. Accepting the prompt and then closing the share sheet without
 *   picking a target counts as a decline too.
 * - Both are also separated by [EngagementPolicy.minPromptsGap] (14 days): no share prompt right
 *   after a review launch and no review right after a share prompt.
 *
 * Opportunities are not wasted: when a milestone is reached but nothing can be shown because
 * another flow is in progress, or because neither the review nor the share prompt could launch
 * (e.g. no foreground activity when triggered from a notification action), the prompt is marked
 * as pending ([EngagementData.pendingPromptAt], refreshed by every new such milestone but not by
 * calls that are eligible only because of it) and the next call within
 * [EngagementPolicy.pendingPromptTtl] (48 hours) is eligible regardless of its milestone. An
 * expired pending prompt is ignored and cleared. Nothing is marked as pending when the policy
 * allows neither prompt.
 *
 * A concurrent call made while a flow is in progress is counted and succeeds with
 * [EngagementResult.None] without being queued (it may still mark a pending prompt).
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
    engagementPolicy: EngagementPolicy,
    firstInstallTimeMillis: () -> Long,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : EngagementUseCase {

    private val mutex = Mutex()
    private val promptPolicy = PromptPolicy(engagementPolicy, firstInstallTimeMillis, currentTimeMillis)

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

    /**
     * Another flow holds the screen: keeps a reached milestone for a later call instead of losing
     * it, refreshing an already pending prompt (only milestones get here, see [shouldDefer]).
     */
    private suspend fun deferIfEligible(
        engagementData: EngagementData,
        origin: String,
    ): Answer<EngagementResult, Failure> = when {
        engagementData.shouldDefer(origin) -> markPending(EngagementResult.None, isMilestone = true)
        else -> Answer.Success(EngagementResult.None)
    }

    private fun EngagementData.shouldDefer(
        origin: String,
    ): Boolean = isMilestone(origin) && promptPolicy.canPromptAny(this)

    private suspend fun engage(
        engagementData: EngagementData,
        origin: String,
    ): Answer<EngagementResult, Failure> = when {
        !engagementData.isEligible(origin) -> Answer.Success(EngagementResult.None)

        promptPolicy.canPromptReview(engagementData) ->
            rateOrShare(engagementData, isMilestone = engagementData.isMilestone(origin))

        promptPolicy.canPromptShare(engagementData) -> share(isMilestone = engagementData.isMilestone(origin))

        // A pending prompt that no policy allows any more is dropped rather than kept until it expires.
        promptPolicy.hasPendingPrompt(engagementData) -> clearPending()

        else -> Answer.Success(EngagementResult.None)
    }

    /** Counts the occurrence and drops an expired pending prompt in the same write. */
    private suspend fun track(
        origin: String,
    ): Answer<EngagementData, Failure> = engagementDataRepository.updateEngagementData { engagementData ->
        engagementData.increment(origin).clearExpiredPendingPrompt()
    }

    private fun EngagementData.increment(
        origin: String,
    ): EngagementData = copy(counts = counts + (origin to (counts[origin] ?: 0) + 1))

    private fun EngagementData.clearExpiredPendingPrompt(): EngagementData = when {
        pendingPromptAt != null && !promptPolicy.hasPendingPrompt(this) -> copy(pendingPromptAt = null)
        else -> this
    }

    private fun EngagementData.isEligible(
        origin: String,
    ): Boolean = promptPolicy.hasPendingPrompt(this) || isMilestone(origin)

    private fun EngagementData.isMilestone(
        origin: String,
    ): Boolean = engagementEligibility.isEligible(origin, counts[origin] ?: 0)

    /**
     * The review clears the pending prompt and records the attempt itself (see [RateAppUseCase]).
     * [isMilestone] tells whether this call reached a milestone or runs only for a pending prompt.
     */
    private suspend fun rateOrShare(
        engagementData: EngagementData,
        isMilestone: Boolean,
    ): Answer<EngagementResult, Failure> = rateAppUseCase(RateAppUseCase.Params).coFlatMap { reviewUnavailable ->
        when {
            !reviewUnavailable -> Answer.Success(EngagementResult.ReviewLaunched)
            promptPolicy.canPromptShare(engagementData) -> share(isMilestone).then { it.orReviewUnavailable() }
            else -> markPending(EngagementResult.ReviewUnavailable, isMilestone)
        }
    }

    private fun EngagementResult.orReviewUnavailable(): EngagementResult = when (this) {
        EngagementResult.ShareNotChosen, EngagementResult.None -> EngagementResult.ReviewUnavailable
        else -> this
    }

    private suspend fun share(
        isMilestone: Boolean,
    ): Answer<EngagementResult, Failure> =
        when (shareAppUseCase(ShareAppUseCase.Params(withPrompt = true)).getValueOrNull()) {
            is InAppShareResult.TargetChosen -> recordSharePrompt(EngagementResult.ShareChosen) { now ->
                copy(sharedAt = now)
            }

            // "Not now", or accepted and then the sheet closed without a target: both count towards
            // the declines cap and wait for the cooldown.
            InAppShareResult.Declined,
            InAppShareResult.Dismissed,
            -> recordSharePrompt(EngagementResult.ShareNotChosen) {
                copy(declines = declines + 1)
            }

            // The prompt could not be shown: kept pending for the next call.
            null -> markPending(EngagementResult.ShareNotChosen, isMilestone)
        }

    /** Records the shown prompt ([update] receives the current time) and clears the pending prompt. */
    private suspend fun recordSharePrompt(
        result: EngagementResult,
        update: ShareState.(now: Long) -> ShareState,
    ): Answer<EngagementResult, Failure> = updateEngagementData {
        val now = currentTimeMillis()
        copy(share = share.update(now).copy(lastPromptAt = now), pendingPromptAt = null)
    }.then { result }

    /**
     * Marks the pending prompt. A milestone ([isMilestone]) stamps it with the current time,
     * starting its TTL anew; a call eligible only because of the pending prompt keeps its
     * timestamp, so repeated calls that cannot show anything never extend the TTL.
     */
    private suspend fun markPending(
        result: EngagementResult,
        isMilestone: Boolean,
    ): Answer<EngagementResult, Failure> = updateEngagementData {
        val now = currentTimeMillis()
        copy(pendingPromptAt = if (isMilestone) now else pendingPromptAt ?: now)
    }.then { result }

    private suspend fun clearPending(): Answer<EngagementResult, Failure> =
        updateEngagementData { copy(pendingPromptAt = null) }.then { EngagementResult.None }

    /** Applies [update] atomically to the latest stored data, which may have changed while a prompt was on screen. */
    private suspend fun updateEngagementData(
        update: EngagementData.() -> EngagementData,
    ): Answer<EngagementData, Failure> = engagementDataRepository.updateEngagementData { engagementData ->
        engagementData.update()
    }
}

/** Applies [EngagementPolicy] for [EngagementUseCaseImpl]; see [EngagementUseCase]. */
private class PromptPolicy(
    private val engagementPolicy: EngagementPolicy,
    firstInstallTimeMillis: () -> Long,
    private val currentTimeMillis: () -> Long,
) {

    private val installTimeMillis: Long by lazy(firstInstallTimeMillis)

    fun canPromptAny(
        engagementData: EngagementData,
    ): Boolean = canPromptReview(engagementData) || canPromptShare(engagementData)

    fun canPromptReview(
        engagementData: EngagementData,
    ): Boolean = with(engagementData) {
        hasMinInstallAgeElapsed() &&
            !review.hasRated &&
            !hasReachedReviewAttemptsLimit() &&
            hasReviewCooldownElapsed() &&
            hasGapSinceSharePromptElapsed()
    }

    fun canPromptShare(
        engagementData: EngagementData,
    ): Boolean = with(engagementData) {
        hasMinInstallAgeElapsed() &&
            !share.hasShared &&
            !hasReachedShareDeclinesLimit() &&
            hasShareCooldownElapsed() &&
            hasGapSinceReviewElapsed()
    }

    /** `true` while a pending prompt exists and is younger than [EngagementPolicy.pendingPromptTtl]. */
    fun hasPendingPrompt(
        engagementData: EngagementData,
    ): Boolean = engagementData.pendingPromptAt?.let { pendingPromptAt ->
        !hasElapsed(pendingPromptAt, engagementPolicy.pendingPromptTtl)
    } ?: false

    private fun hasMinInstallAgeElapsed(): Boolean = hasElapsed(installTimeMillis, engagementPolicy.minInstallAge)

    private fun EngagementData.hasReachedReviewAttemptsLimit(): Boolean =
        review.attempts >= engagementPolicy.maxReviewAttempts

    private fun EngagementData.hasReviewCooldownElapsed(): Boolean =
        hasElapsed(review.lastAt, engagementPolicy.reviewCooldown)

    private fun EngagementData.hasGapSinceSharePromptElapsed(): Boolean =
        hasElapsed(share.lastPromptAt, engagementPolicy.minPromptsGap)

    private fun EngagementData.hasReachedShareDeclinesLimit(): Boolean =
        share.declines >= engagementPolicy.maxShareDeclines

    private fun EngagementData.hasShareCooldownElapsed(): Boolean =
        hasElapsed(share.lastPromptAt, engagementPolicy.shareCooldown)

    private fun EngagementData.hasGapSinceReviewElapsed(): Boolean =
        hasElapsed(review.lastAt, engagementPolicy.minPromptsGap)

    /** `true` when [interval] has passed since [since], or when there is no [since]. */
    private fun hasElapsed(
        since: Long?,
        interval: Duration,
    ): Boolean = since?.let { currentTimeMillis() - it >= interval.inWholeMilliseconds } ?: true
}
