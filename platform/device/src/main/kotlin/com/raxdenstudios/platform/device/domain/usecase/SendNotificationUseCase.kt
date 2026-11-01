package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.repository.NotificationRepository
import com.raxdenstudios.platform.device.domain.model.NotificationData

interface SendNotificationUseCase : SuspendUseCase<SendNotificationUseCase.Params, Unit> {
    data class Params(val notificationData: NotificationData)
}

class SendNotificationUseCaseImpl(
    private val notificationRepository: NotificationRepository,
) : SendNotificationUseCase {

    override suspend fun invoke(params: SendNotificationUseCase.Params): Answer<Unit, Failure> =
        notificationRepository.send(params.notificationData)
}
