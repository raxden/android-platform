package com.raxdenstudios.platform.notification.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository

/**
 * Whether the app can post notifications right now, read from the application context: safe with no activity alive
 * (a message handled in the background), unlike [GetNotificationPermissionStateUseCase].
 */
interface AreNotificationsEnabledUseCase : SuspendUseCase<AreNotificationsEnabledUseCase.Params, Boolean> {

    data object Params
}

internal class AreNotificationsEnabledUseCaseImpl(
    private val repository: NotificationPermissionRepository,
) : AreNotificationsEnabledUseCase {

    override suspend fun invoke(
        params: AreNotificationsEnabledUseCase.Params,
    ): Answer<Boolean, Failure> = repository.areNotificationsEnabled()
}
