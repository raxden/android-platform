package com.raxdenstudios.platform.engagement.di.module

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSourceImpl
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepositoryImpl
import com.raxdenstudios.platform.engagement.domain.DefaultEngagementEligibility
import com.raxdenstudios.platform.engagement.domain.EngagementEligibility
import com.raxdenstudios.platform.engagement.domain.EngagementUseCase
import com.raxdenstudios.platform.engagement.domain.EngagementUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.RateAppUseCase
import com.raxdenstudios.platform.engagement.domain.RateAppUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.ShareAppUseCase
import com.raxdenstudios.platform.engagement.domain.ShareAppUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class EngagementDIModule : DIModule {

    override fun get(): Module = module {
        singleOf(::EngagementDataLocalDataSourceImpl) bind EngagementDataLocalDataSource::class
        factoryOf(::EngagementDataRepositoryImpl) bind EngagementDataRepository::class
        factoryOf(::DefaultEngagementEligibility) bind EngagementEligibility::class
        singleOf(::EngagementUseCaseImpl) bind EngagementUseCase::class
        factoryOf(::RateAppUseCaseImpl) bind RateAppUseCase::class
        factoryOf(::ShareAppUseCaseImpl) bind ShareAppUseCase::class
    }
}
