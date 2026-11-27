package com.raxdenstudios.platform.notification.domain.model

/**
 * Outcome of asking for the notification permission.
 *
 * @property state The permission state afterwards (or the current one when nothing was shown).
 * @property isDialogShown Whether the system dialog was actually shown and answered; only then there is an answer to
 * report.
 */
data class NotificationPermissionDialogResult(
    val state: NotificationPermissionState,
    val isDialogShown: Boolean,
)
