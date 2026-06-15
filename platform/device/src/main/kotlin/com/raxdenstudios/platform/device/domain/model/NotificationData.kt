package com.raxdenstudios.platform.device.domain.model

import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.raxdenstudios.commons.core.ext.EMPTY

data class NotificationData(
    val id: Int = 0,
    val smallIcon: Int = 0,
    val contentTitle: String = String.EMPTY,
    val contentText: String = String.EMPTY,
    val bigText: String = String.EMPTY,
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val autoCancel: Boolean = true,
    val actions: List<Action> = emptyList(),
    val channel: NotificationChannel,
    val contentIntent: PendingIntent? = null,
) {

    data class Action(
        val icon: Int,
        val title: String,
        val intent: PendingIntent,
    )
}
