package com.raxdenstudios.platform.device.domain.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.model.NotificationData

interface NotificationRepository {
    suspend fun send(notification: NotificationData): Answer<Unit, Failure>
}
