package com.raxdenstudios.platform.device

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

interface NotificationTracker {
    fun channelCreated()
    fun notificationBuilt()
    fun notificationSent()
    fun notificationSendError()
}

internal class NotificationTrackerImpl(
    private val firebaseAnalytics: FirebaseAnalytics,
): NotificationTracker {

    override fun channelCreated() {
        firebaseAnalytics.logEvent(NOTIFICATION_CHANNEL_CREATED) {}
    }

    override fun notificationBuilt() {
        firebaseAnalytics.logEvent(NOTIFICATION_BUILT) {}
    }

    override fun notificationSent() {
        firebaseAnalytics.logEvent(NOTIFICATION_SENT) {}
    }

    override fun notificationSendError() {
        firebaseAnalytics.logEvent(NOTIFICATION_SEND_ERROR) {}
    }

    private companion object {
        const val NOTIFICATION_CHANNEL_CREATED = "notif_channel_created"
        const val NOTIFICATION_BUILT = "notif_built"
        const val NOTIFICATION_SENT = "notif_sent"
        const val NOTIFICATION_SEND_ERROR = "notif_send_error"
    }
}
