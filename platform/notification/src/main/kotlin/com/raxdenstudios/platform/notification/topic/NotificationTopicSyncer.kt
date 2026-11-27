package com.raxdenstudios.platform.notification.topic

import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.core.ext.onSuccess
import com.raxdenstudios.platform.notification.domain.SyncNotificationTopicUseCase
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicPolicy
import com.raxdenstudios.platform.notification.domain.model.NotificationTopicSync
import timber.log.Timber

/**
 * Syncs the topic with the device time zone ([SyncNotificationTopicUseCase]) and reports a change to the app's
 * [NotificationTopicSyncListener], for both entry points: the app start and [NotificationTopicTimeZoneReceiver]. A
 * failure is only logged; the next sync retries. Does nothing while the app binds no [NotificationTopicPolicy].
 */
interface NotificationTopicSyncer {

    /**
     * Runs one topic sync, waiting for any sync already in progress. Call it from the app start (on a background
     * scope) and from the time zone receiver. Does not return a result: a failed sync is only logged, and the next
     * sync retries it.
     */
    suspend fun sync()
}

internal class NotificationTopicSyncerImpl(
    private val syncNotificationTopicUseCase: SyncNotificationTopicUseCase,
    private val policy: NotificationTopicPolicy?,
    private val listener: NotificationTopicSyncListener?,
) : NotificationTopicSyncer {

    override suspend fun sync() {
        if (policy == null) {
            Timber.d("Notification topic sync skipped: no NotificationTopicPolicy is bound")
            return
        }
        syncNotificationTopicUseCase(SyncNotificationTopicUseCase.Params)
            .onSuccess(::onSynced)
            .onFailure { failure -> Timber.w("Unable to sync the notification topic: ${failure.message}") }
    }

    private fun onSynced(
        sync: NotificationTopicSync,
    ) {
        if (sync.hasChanges) listener?.onTopicChanged(sync)
    }
}
