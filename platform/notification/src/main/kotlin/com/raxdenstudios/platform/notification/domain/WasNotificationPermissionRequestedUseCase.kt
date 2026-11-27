package com.raxdenstudios.platform.notification.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository

/**
 * Whether this app ever showed the system notification permission dialog. Android does not tell a permission never
 * asked from one asked and denied, so an app that must treat them differently (e.g. prompt only users never asked)
 * reads it here instead of the module's preferences file.
 */
interface WasNotificationPermissionRequestedUseCase :
    SuspendUseCase<WasNotificationPermissionRequestedUseCase.Params, Boolean> {

    data object Params
}

internal class WasNotificationPermissionRequestedUseCaseImpl(
    private val repository: NotificationPermissionRepository,
) : WasNotificationPermissionRequestedUseCase {

    override suspend fun invoke(
        params: WasNotificationPermissionRequestedUseCase.Params,
    ): Answer<Boolean, Failure> = repository.wasRequested()
}
