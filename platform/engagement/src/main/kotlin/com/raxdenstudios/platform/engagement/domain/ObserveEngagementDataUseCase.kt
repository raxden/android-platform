package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.FlowUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.flow.Flow

/** Observes the persisted [EngagementData], e.g. to know whether the app was already rated. */
interface ObserveEngagementDataUseCase : FlowUseCase<ObserveEngagementDataUseCase.Params, EngagementData> {

    data object Params
}

internal class ObserveEngagementDataUseCaseImpl(private val engagementDataRepository: EngagementDataRepository) :
    ObserveEngagementDataUseCase {

    override fun invoke(params: ObserveEngagementDataUseCase.Params): Flow<Answer<EngagementData, Failure>> =
        engagementDataRepository.observeEngagementData()
}
