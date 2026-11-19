package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository

/**
 * Records that the user explicitly went to rate the app, e.g. opened the Play Store rating page
 * from a settings entry or after [RateAppUseCase] asked for the manual route, by setting
 * `ReviewState.ratedAt` to now (a later call moves it forward). From then on [EngagementUseCase]
 * never requests the in-app review again; the share prompt keeps its own policy.
 */
interface MarkAppAsRatedUseCase : SuspendUseCase<MarkAppAsRatedUseCase.Params, Unit> {

    data object Params
}

internal class MarkAppAsRatedUseCaseImpl(
    private val engagementDataRepository: EngagementDataRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : MarkAppAsRatedUseCase {

    override suspend fun invoke(
        params: MarkAppAsRatedUseCase.Params,
    ): Answer<Unit, Failure> = engagementDataRepository.updateEngagementData { engagementData ->
        engagementData.copy(review = engagementData.review.copy(ratedAt = currentTimeMillis()))
    }.then { }
}
