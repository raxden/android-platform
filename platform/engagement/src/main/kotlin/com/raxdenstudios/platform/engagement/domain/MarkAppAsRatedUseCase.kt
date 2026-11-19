package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository

/**
 * Records that the user explicitly went to rate the app, e.g. opened the Play Store rating page
 * from a settings entry or after [RateAppUseCase] asked for the manual route. From then on
 * [EngagementUseCase] never requests the in-app review again; the share prompt keeps its own
 * policy.
 */
interface MarkAppAsRatedUseCase : SuspendUseCase<MarkAppAsRatedUseCase.Params, Unit> {

    data object Params
}

internal class MarkAppAsRatedUseCaseImpl(
    private val engagementDataRepository: EngagementDataRepository,
) : MarkAppAsRatedUseCase {

    override suspend fun invoke(
        params: MarkAppAsRatedUseCase.Params,
    ): Answer<Unit, Failure> = engagementDataRepository.getEngagementData().coFlatMap { engagementData ->
        engagementDataRepository.updateEngagementData(engagementData.copy(hasRated = true))
    }
}
