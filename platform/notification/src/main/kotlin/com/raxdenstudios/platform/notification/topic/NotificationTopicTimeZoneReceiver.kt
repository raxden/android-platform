package com.raxdenstudios.platform.notification.topic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raxdenstudios.commons.android.ext.goAsync
import com.raxdenstudios.commons.core.ext.onFailure
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.ext.coRunCatching
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Moves the device to the topic of its new UTC offset as soon as the time zone changes (travelling, or a manual
 * change), instead of waiting for the next app start. Declared in this module's manifest.
 *
 * The system instantiates it: hence [KoinComponent] and `by inject()`. The sync runs on
 * [CoroutineScopeProvider.default] inside [goAsync], which keeps the process alive until it finishes; a `goAsync`
 * broadcast must finish within about 10 s, so the sync is abandoned after [SYNC_TIMEOUT] (it is retried on the next
 * app start). Daylight saving transitions send no broadcast: the next process start picks them up.
 */
class NotificationTopicTimeZoneReceiver :
    BroadcastReceiver(),
    KoinComponent {

    private val notificationTopicSyncer: NotificationTopicSyncer by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) return

        goAsync(
            scope = coroutineScopeProvider.default,
            timeout = SYNC_TIMEOUT,
            onTimeout = { Timber.w("Notification topic sync timed out after $SYNC_TIMEOUT") },
        ) {
            syncSafely()
        }
    }

    // A failure (Firebase not initialised, say) is logged, never a crash in the broadcast.
    private suspend fun syncSafely() {
        coRunCatching { notificationTopicSyncer.sync() }
            .onFailure { error -> Timber.e(error, "Notification topic sync failed") }
    }

    private companion object {
        val SYNC_TIMEOUT = 8.seconds
    }
}
