package com.raxdenstudios.platform.notification.domain.model

/** Whether the app may post notifications and, when it may not, what can still be done about it. */
enum class NotificationPermissionState {

    /** Notifications are allowed. */
    Granted,

    /** Android 13+: not granted yet, and the system dialog can still be shown. */
    Requestable,

    /** Android 13+: not granted and the system dialog no longer shows; only the system settings can turn them on. */
    Blocked,

    /**
     * The user turned the app's notifications off in the system settings: below Android 13 (no runtime permission),
     * or on Android 13+ with the permission granted. Only the system settings can turn them back on.
     */
    Disabled,
    ;

    /** Whether notifications can be posted. */
    val isGranted: Boolean
        get() = this == Granted

    /** Whether the runtime permission itself is what is missing (Android 13+, not granted). */
    val isRuntimePermissionMissing: Boolean
        get() = this == Requestable || this == Blocked
}
