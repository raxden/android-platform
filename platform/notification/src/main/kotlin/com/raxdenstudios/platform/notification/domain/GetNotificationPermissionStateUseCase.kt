package com.raxdenstudios.platform.notification.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationPermissionState

/**
 * The current [NotificationPermissionState]. On Android 13+ a permission that is not granted is
 * [NotificationPermissionState.Requestable] while the system still shows its dialog: never asked, or denied once
 * (rationale). Once asked and no longer showing the rationale it is permanently denied
 * ([NotificationPermissionState.Blocked]). Granted but with the app's notifications turned off in the system settings
 * it is [NotificationPermissionState.Disabled]: no dialog can fix that.
 */
interface GetNotificationPermissionStateUseCase :
    SuspendUseCase<GetNotificationPermissionStateUseCase.Params, NotificationPermissionState> {

    data object Params
}

internal class GetNotificationPermissionStateUseCaseImpl(
    private val repository: NotificationPermissionRepository,
) : GetNotificationPermissionStateUseCase {

    override suspend fun invoke(
        params: GetNotificationPermissionStateUseCase.Params,
    ): Answer<NotificationPermissionState, Failure> = repository.areNotificationsEnabled().coFlatMap { enabled ->
        if (enabled) Answer.Success(NotificationPermissionState.Granted) else missingPermissionState()
    }

    private suspend fun missingPermissionState(): Answer<NotificationPermissionState, Failure> =
        repository.isRuntimePermission().coFlatMap { isRuntime ->
            if (isRuntime) runtimePermissionState() else Answer.Success(NotificationPermissionState.Disabled)
        }

    // Granted but off: the user turned the app's notifications off in the system settings, the only place to fix it.
    private suspend fun runtimePermissionState(): Answer<NotificationPermissionState, Failure> =
        repository.isPermissionGranted().coFlatMap { granted ->
            if (granted) Answer.Success(NotificationPermissionState.Disabled) else deniedPermissionState()
        }

    private suspend fun deniedPermissionState(): Answer<NotificationPermissionState, Failure> =
        repository.shouldShowRationale().coFlatMap { rationale ->
            if (rationale) {
                Answer.Success(NotificationPermissionState.Requestable)
            } else {
                repository.wasRequested().then { requested ->
                    if (requested) NotificationPermissionState.Blocked else NotificationPermissionState.Requestable
                }
            }
        }
}
