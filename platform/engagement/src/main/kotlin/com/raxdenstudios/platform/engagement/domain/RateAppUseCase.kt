package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppReview
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository

/**
 * Launches the in-app review flow unconditionally. It does not decide whether the prompt should
 * be shown: that decision lives in [EngagementUseCase]. When the flow is launched the app is
 * marked as rated. The use case succeeds with `true` when the caller should navigate to the Play
 * Store rating page instead: the review flow could not be launched and the app has not been
 * rated yet.
 */
interface RateAppUseCase : SuspendUseCase<RateAppUseCase.Params, Boolean> {

    data object Params
}

class RateAppUseCaseImpl(
    private val inAppReview: InAppReview,
    private val engagementDataRepository: EngagementDataRepository,
) : RateAppUseCase {

    override suspend fun invoke(params: RateAppUseCase.Params): Answer<Boolean, Failure> = when {
        inAppReview.launch().isSuccess -> markAsRated()
        else -> Answer.Success(true)
    }

    private suspend fun markAsRated(): Answer<Boolean, Failure> =
        engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
            engagementDataRepository.updateEngagementData(engagementData.copy(hasRated = true)).then { false }
        }
}
