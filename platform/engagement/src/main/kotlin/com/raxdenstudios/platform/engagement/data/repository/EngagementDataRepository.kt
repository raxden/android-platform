package com.raxdenstudios.platform.engagement.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import kotlinx.coroutines.flow.Flow

interface EngagementDataRepository {

    fun observeEngagementData(): Flow<Answer<EngagementData, Failure>>
    suspend fun updateEngagementData(engagementData: EngagementData): Answer<Unit, Failure>
    suspend fun getEngagementData(): Answer<EngagementData, Failure>
}

class EngagementDataRepositoryImpl(private val engagementDataLocalDataSource: EngagementDataLocalDataSource) :
    EngagementDataRepository {

    override fun observeEngagementData(): Flow<Answer<EngagementData, Failure>> =
        engagementDataLocalDataSource.observeEngagementData()

    override suspend fun getEngagementData(): Answer<EngagementData, Failure> =
        engagementDataLocalDataSource.getEngagementData()

    override suspend fun updateEngagementData(engagementData: EngagementData): Answer<Unit, Failure> =
        engagementDataLocalDataSource.updateEngagementData(engagementData)
}
