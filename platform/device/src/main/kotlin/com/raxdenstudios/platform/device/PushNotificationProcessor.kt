package com.raxdenstudios.platform.device

import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.messaging.RemoteMessage
import com.raxdenstudios.commons.core.ext.EMPTY
import com.raxdenstudios.platform.device.domain.model.NotificationData

interface PushNotificationProcessor {
    fun map(message: RemoteMessage): NotificationData?
}

internal class DefaultPushNotificationProcessor :
    PushNotificationProcessor {

    override fun map(message: RemoteMessage): NotificationData {
        return NotificationData(
            channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            contentTitle = message.notification?.title ?: message.data["title"] ?: String.EMPTY,
            contentText = message.notification?.body ?: message.data["body"] ?: String.EMPTY,
        )
    }

    private companion object {
        const val CHANNEL_ID = "channel_id"
        const val CHANNEL_NAME = "channel_name"
    }
}
