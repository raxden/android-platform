package com.raxdenstudios.platform.device

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.device.domain.model.NotificationData

interface NotificationSender {

    fun hasPermission(): Boolean
    fun send(notification: NotificationData): Answer<Unit, Exception>
}

class NotificationSenderImpl(
    private val context: Context,
    private val tracker: NotificationTracker,
) : NotificationSender {

    override fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun send(notification: NotificationData): Answer<Unit, Exception> = try {
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
        Answer.Failure(e)
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
