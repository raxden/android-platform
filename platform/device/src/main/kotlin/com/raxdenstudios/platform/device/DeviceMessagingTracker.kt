package com.raxdenstudios.platform.device

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

interface DeviceMessagingTracker {
    fun topicSubscribed(topic: String)
    fun tokenRefreshed()
    fun messageReceived()
    fun messagesDeleted()
    fun tokenSentToServer()
    fun tokenSendFailed()
}

class DeviceMessagingTrackerImpl(
    private val firebaseAnalytics: FirebaseAnalytics,
): DeviceMessagingTracker {

    override fun topicSubscribed(topic: String) {
        firebaseAnalytics.logEvent(FCM_TOPIC_SUBSCRIBED) {
            param("topic", topic)
        }
    }

    override fun tokenRefreshed() {
        firebaseAnalytics.logEvent(FCM_TOKEN_REFRESHED) {}
    }

    override fun messageReceived() {
        firebaseAnalytics.logEvent(FCM_MESSAGE_RECEIVED) {}
    }

    override fun messagesDeleted() {
        firebaseAnalytics.logEvent(FCM_MESSAGES_DELETED) {}
    }

    override fun tokenSentToServer() {
        firebaseAnalytics.logEvent(FCM_TOKEN_SENT_TO_SERVER) {}
    }

    override fun tokenSendFailed() {
        firebaseAnalytics.logEvent(FCM_TOKEN_SEND_FAILED) {}
    }

    private companion object {
        const val FCM_TOPIC_SUBSCRIBED = "fcm_topic_subscribed"
        const val FCM_TOKEN_REFRESHED = "fcm_token_refreshed"
        const val FCM_MESSAGE_RECEIVED = "fcm_message_received"
        const val FCM_MESSAGES_DELETED = "fcm_messages_deleted"
        const val FCM_TOKEN_SENT_TO_SERVER = "fcm_token_sent_to_server"
        const val FCM_TOKEN_SEND_FAILED = "fcm_token_send_failed"
    }
}
