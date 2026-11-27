package com.raxdenstudios.platform.notification.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.notification.data.datasource.NotificationPermissionLocalDataSource
import com.raxdenstudios.platform.notification.permission.NotificationPermissionLauncher

/** The system facts about the notification permission, plus whether the app ever asked for it. */
internal interface NotificationPermissionRepository {

    fun areNotificationsEnabled(): Answer<Boolean, Failure>

    fun isRuntimePermission(): Answer<Boolean, Failure>

    fun isPermissionGranted(): Answer<Boolean, Failure>

    fun shouldShowRationale(): Answer<Boolean, Failure>

    suspend fun wasRequested(): Answer<Boolean, Failure>

    /**
     * Shows the system dialog and, once answered, records that it was shown. Returns whether it was shown: false when
     * there was no runtime permission, it was already granted or there was no screen to show it on.
     */
    suspend fun request(): Answer<Boolean, Failure>
}

internal class NotificationPermissionRepositoryImpl(
    private val launcher: NotificationPermissionLauncher,
    private val localDataSource: NotificationPermissionLocalDataSource,
) : NotificationPermissionRepository {

    override fun areNotificationsEnabled(): Answer<Boolean, Failure> =
        Answer.Success(launcher.areNotificationsEnabled())

    override fun isRuntimePermission(): Answer<Boolean, Failure> = Answer.Success(launcher.isRuntimePermission())

    override fun isPermissionGranted(): Answer<Boolean, Failure> = Answer.Success(launcher.isPermissionGranted())

    override fun shouldShowRationale(): Answer<Boolean, Failure> = Answer.Success(launcher.shouldShowRationale())

    override suspend fun wasRequested(): Answer<Boolean, Failure> = localDataSource.wasRequested()

    override suspend fun request(): Answer<Boolean, Failure> {
        val isDialogShown = launcher.launch()
        // Failing to remember the request must not lose the user's answer.
        if (isDialogShown) localDataSource.markAsRequested()
        return Answer.Success(isDialogShown)
    }
}
