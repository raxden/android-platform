package com.raxdenstudios.platform.engagement.di.module

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSourceImpl
import com.raxdenstudios.platform.engagement.data.datasource.local.install.firstInstallTimeMillis
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapperImpl
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepositoryImpl
import com.raxdenstudios.platform.engagement.domain.DefaultEngagementEligibility
import com.raxdenstudios.platform.engagement.domain.DefaultShareContentProvider
import com.raxdenstudios.platform.engagement.domain.EngagementUseCase
import com.raxdenstudios.platform.engagement.domain.EngagementUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.MarkAppAsRatedUseCase
import com.raxdenstudios.platform.engagement.domain.MarkAppAsRatedUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.ObserveEngagementDataUseCase
import com.raxdenstudios.platform.engagement.domain.ObserveEngagementDataUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.RateAppUseCase
import com.raxdenstudios.platform.engagement.domain.RateAppUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.ShareAppUseCase
import com.raxdenstudios.platform.engagement.domain.ShareAppUseCaseImpl
import com.raxdenstudios.platform.engagement.domain.model.EngagementPolicy
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Apps may bind their own `EngagementEligibility` and `ShareContentProvider`; when absent the
 * generic defaults are used. [EngagementPolicy] is bound here with its defaults as a `factory`, so
 * an app module loaded afterwards can override it.
 *
 * Persistence is self-contained: the module creates its own Room database
 * (`platform_engagement.db`), kept as a single, so consumers bind nothing for it.
 */
class EngagementDIModule : DIModule {

    override fun get(): Module = module {
        single<EngagementDatabase> { EngagementDatabase.create(androidContext()) }
        factoryOf(::EngagementEntityDataMapperImpl) bind EngagementEntityDataMapper::class
        factoryOf(::EngagementDataLocalDataSourceImpl) bind EngagementDataLocalDataSource::class
        factoryOf(::EngagementDataRepositoryImpl) bind EngagementDataRepository::class
        factoryOf(::ObserveEngagementDataUseCaseImpl) bind ObserveEngagementDataUseCase::class
        factory<RateAppUseCase> {
            RateAppUseCaseImpl(
                inAppReview = get(),
                engagementDataRepository = get(),
            )
        }
        factory<MarkAppAsRatedUseCase> {
            MarkAppAsRatedUseCaseImpl(
                engagementDataRepository = get(),
            )
        }
        factory<ShareAppUseCase> {
            ShareAppUseCaseImpl(
                shareContentProvider = getOrNull() ?: DefaultShareContentProvider(androidContext()),
                inAppShare = get(),
            )
        }
        factory { EngagementPolicy() }
        single<EngagementUseCase> {
            val context = androidContext()
            EngagementUseCaseImpl(
                rateAppUseCase = get(),
                shareAppUseCase = get(),
                engagementDataRepository = get(),
                engagementEligibility = getOrNull() ?: DefaultEngagementEligibility(),
                engagementPolicy = get(),
                firstInstallTimeMillis = context::firstInstallTimeMillis,
            )
        }
    }
}
