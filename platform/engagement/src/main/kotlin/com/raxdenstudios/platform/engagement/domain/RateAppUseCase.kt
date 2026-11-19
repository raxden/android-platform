package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppReview
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository

/**
 * Launches the in-app review flow unconditionally. It does not decide whether the prompt should
 * be shown: that decision lives in [EngagementUseCase]. When the flow is launched the attempt is
 * recorded (`reviewAttempts` and `lastReviewAt`, which drive the review cooldown and cap) and any
 * pending prompt is cleared; the app is not marked as rated, since Play never reports whether the
 * dialog was displayed. The use case succeeds with `true` when the caller should navigate to the
 * Play Store rating page instead because the review flow could not be launched; callers that do
 * so should then report it through [MarkAppAsRatedUseCase].
 */
interface RateAppUseCase : SuspendUseCase<RateAppUseCase.Params, Boolean> {

    data object Params
}

internal class RateAppUseCaseImpl(
    private val inAppReview: InAppReview,
    private val engagementDataRepository: EngagementDataRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : RateAppUseCase {

    override suspend fun invoke(
        params: RateAppUseCase.Params,
    ): Answer<Boolean, Failure> = when {
        inAppReview.launch().isSuccess -> recordReviewAttempt()
        else -> Answer.Success(true)
    }

    private suspend fun recordReviewAttempt(): Answer<Boolean, Failure> =
        engagementDataRepository.updateEngagementData { engagementData ->
            engagementData.copy(
                reviewAttempts = engagementData.reviewAttempts + 1,
                lastReviewAt = currentTimeMillis(),
                pendingPrompt = false,
            )
        }.then { false }
}
