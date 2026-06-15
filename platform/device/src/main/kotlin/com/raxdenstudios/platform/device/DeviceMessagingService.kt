package com.raxdenstudios.platform.device

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.platform.device.domain.usecase.SaveFCMTokenUseCase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DeviceMessagingService : FirebaseMessagingService() {

    private val coroutineScopeProvider: CoroutineScopeProvider by inject()
    private val saveFCMTokenUseCase: SaveFCMTokenUseCase by inject()
    private val notificationSender: NotificationSender by inject()
    private val pushNotificationProcessor: PushNotificationProcessor by inject()
    private val tracker: DeviceMessagingTracker by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        tracker.tokenRefreshed()

        saveFCMToken(token)
        // TODO: Send token to your backend server
    }

    private fun saveFCMToken(token: String) {
        coroutineScopeProvider.io.launch {
            saveFCMTokenUseCase(SaveFCMTokenUseCase.Params(token))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        tracker.messageReceived()

        val notificationData = pushNotificationProcessor.map(message)
        if (notificationData != null)
            notificationSender.send(notificationData)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        tracker.messagesDeleted()
    }
}
