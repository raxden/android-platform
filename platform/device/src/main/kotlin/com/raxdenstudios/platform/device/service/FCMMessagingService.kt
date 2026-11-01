package com.raxdenstudios.platform.device.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.platform.device.service.mapper.FCMNotificationMapper
import com.raxdenstudios.platform.device.domain.usecase.SendNotificationUseCase
import com.raxdenstudios.platform.device.domain.usecase.SaveFCMTokenUseCase
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class FCMMessagingService : FirebaseMessagingService() {

    private val dispatcherProvider: DispatcherProvider by inject()
    private val saveFCMTokenUseCase: SaveFCMTokenUseCase by inject()
    private val sendNotificationUseCase: SendNotificationUseCase by inject()
    private val fcmNotificationMapper: FCMNotificationMapper by inject()
    private val tracker: FCMMessagingTracker by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        tracker.tokenRefreshed()

        saveFCMToken(token)
    }

    // FCM callbacks run on a worker thread; finish local work before returning.
    private fun saveFCMToken(token: String) {
        runBlocking(dispatcherProvider.io) {
            saveFCMTokenUseCase(SaveFCMTokenUseCase.Params(token))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        tracker.messageReceived()

        val notificationData = fcmNotificationMapper.map(message)
        if (notificationData != null) {
            runBlocking(dispatcherProvider.io) {
                sendNotificationUseCase(SendNotificationUseCase.Params(notificationData))
            }
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        tracker.messagesDeleted()
    }
}
