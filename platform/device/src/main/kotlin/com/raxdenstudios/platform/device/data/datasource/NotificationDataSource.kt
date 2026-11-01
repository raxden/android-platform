package com.raxdenstudios.platform.device.data.datasource

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.model.NotificationData

interface NotificationDataSource {

    suspend fun send(notification: NotificationData): Answer<Unit, Failure>
}

internal class NotificationDataSourceImpl(
    private val context: Context,
    private val tracker: NotificationTracker,
) : NotificationDataSource {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun send(notification: NotificationData): Answer<Unit, Failure> = try {
        val notificationManager = getNotificationManager()
        ensureChannel(notificationManager, notification.channel)

        val pendingIntent = notification.contentIntent ?: getDefaultPendingIntent()

        val builtNotification = NotificationCompat.Builder(context, notification.channel.id)
            .setSmallIcon(notification.smallIcon)
            .setContentTitle(notification.contentTitle)
            .setContentText(notification.contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.bigText))
            .setPriority(notification.priority)
            .setAutoCancel(notification.autoCancel)
            .setContentIntent(pendingIntent)
            .also { builder ->
                notification.actions.forEach { action ->
                    builder.addAction(action.icon, action.title, action.intent)
                }
            }
            .build()

        tracker.notificationBuilt()
        notificationManager.notify(notification.id, builtNotification)
        tracker.notificationSent()
        Answer.Success(Unit)
    } catch (e: Exception) {
        tracker.notificationSendError()
        Answer.Failure(
            when (e) {
                is SecurityException -> Failure.Unauthorized(e.message ?: "Notification permission denied")
                else -> Failure.Unknown(e.message ?: "Failed to send notification")
            }
        )
    }

    private fun getDefaultPendingIntent(): PendingIntent {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, launchIntent, flags)
    }

    private fun getNotificationManager(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel(
        notificationManager: NotificationManager,
        channel: NotificationChannel,
    ) {
        if (notificationManager.getNotificationChannel(channel.id) != null) return
        notificationManager.createNotificationChannel(channel)
        tracker.channelCreated()
    }
}
