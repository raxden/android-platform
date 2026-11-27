package com.raxdenstudios.platform.notification.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.commons.coroutines.ext.coThen
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import com.raxdenstudios.platform.notification.domain.model.NotificationPermissionDialogResult
import com.raxdenstudios.platform.notification.domain.model.NotificationPermissionState

/**
 * Shows the system notification dialog when it can still be shown ([NotificationPermissionState.Requestable]) and
 * returns the state after the user answers; in any other state, or with no screen to show it on, nothing is shown
 * and the current state is returned. [NotificationPermissionDialogResult.isDialogShown] tells both cases apart, so
 * callers report an answer only when there was one. Callers show it only after their own pre-explanation, when the
 * user chose to turn notifications on.
 */
interface LaunchNotificationPermissionDialogUseCase :
    SuspendUseCase<LaunchNotificationPermissionDialogUseCase.Params, NotificationPermissionDialogResult> {

    data object Params
}

internal class LaunchNotificationPermissionDialogUseCaseImpl(
    private val repository: NotificationPermissionRepository,
    private val getNotificationPermissionStateUseCase: GetNotificationPermissionStateUseCase,
) : LaunchNotificationPermissionDialogUseCase {

    override suspend fun invoke(
        params: LaunchNotificationPermissionDialogUseCase.Params,
    ): Answer<NotificationPermissionDialogResult, Failure> = currentState().coFlatMap { state ->
        if (state == NotificationPermissionState.Requestable) {
            requestPermission()
        } else {
            Answer.Success(NotificationPermissionDialogResult(state = state, isDialogShown = false))
        }
    }

    private suspend fun requestPermission(): Answer<NotificationPermissionDialogResult, Failure> =
        repository.request().coFlatMap { isDialogShown ->
            currentState().coThen { state ->
                NotificationPermissionDialogResult(state = state, isDialogShown = isDialogShown)
            }
        }

    private suspend fun currentState(): Answer<NotificationPermissionState, Failure> =
        getNotificationPermissionStateUseCase(GetNotificationPermissionStateUseCase.Params)
}
