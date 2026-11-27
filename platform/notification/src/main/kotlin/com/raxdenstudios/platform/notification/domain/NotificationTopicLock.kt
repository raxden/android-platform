package com.raxdenstudios.platform.notification.domain

import com.raxdenstudios.commons.coroutines.MutexSuspendLock
import com.raxdenstudios.commons.coroutines.SuspendLock

/**
 * Serialises the read-subscribe-save sequence of [SyncNotificationTopicUseCase]: a time zone change that cold-starts
 * the process runs the app-start sync and the receiver sync at the same time, and without the lock both would read
 * the same previous topic. A single instance shared by every caller; every FCM call inside has its own timeout, so it
 * is never held forever.
 */
internal interface NotificationTopicLock : SuspendLock

internal class NotificationTopicLockImpl :
    NotificationTopicLock,
    SuspendLock by MutexSuspendLock()
