package com.raxdenstudios.platform.device.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.datasource.NotificationDataSource
import com.raxdenstudios.platform.device.domain.model.NotificationData
import com.raxdenstudios.platform.device.domain.repository.NotificationRepository

internal class NotificationRepositoryImpl(private val notificationDataSource: NotificationDataSource) :
    NotificationRepository {

    override suspend fun send(notification: NotificationData): Answer<Unit, Failure> =
        notificationDataSource.send(notification)
}
