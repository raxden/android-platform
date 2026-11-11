package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.repository.FCMRepository

interface SubscribeFCMTopicUseCase : SuspendUseCase<SubscribeFCMTopicUseCase.Params, Unit> {
    data class Params(val topic: String)
}

internal class SubscribeFCMTopicUseCaseImpl(private val fcmRepository: FCMRepository) : SubscribeFCMTopicUseCase {

    override suspend fun invoke(params: SubscribeFCMTopicUseCase.Params): Answer<Unit, Failure> =
        fcmRepository.subscribeToTopic(params.topic)
}
