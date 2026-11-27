package com.raxdenstudios.platform.notification.topic

import com.raxdenstudios.platform.notification.domain.model.NotificationTopicSync

/**
 * Optional, bound by the app: told by [NotificationTopicSyncer] whenever a sync changed the subscriptions
 * ([NotificationTopicSync.hasChanges]), e.g. to track it.
 */
fun interface NotificationTopicSyncListener {

    /**
     * Called after a sync that changed the subscriptions, on the coroutine that runs the sync, so it must not block.
     * Not called for syncs without changes nor for failed ones.
     */
    fun onTopicChanged(
        sync: NotificationTopicSync,
    )
}
