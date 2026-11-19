package com.raxdenstudios.platform.engagement.di.module

import android.content.Context
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSourceImpl
import com.raxdenstudios.platform.engagement.data.datasource.local.legacy.LegacyEngagementDataMigrator
import com.raxdenstudios.platform.engagement.data.datasource.local.legacy.LegacyEngagementDataMigratorImpl
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Apps may bind their own `EngagementEligibility` and `ShareContentProvider`; when absent the
 * generic defaults are used.
 *
 * Persistence is self-contained: the module creates its own Room database
 * (`platform_engagement.db`) and reads the legacy `platform_engagement` `SharedPreferences` file
 * only to migrate it, so consumers bind neither.
 */
class EngagementDIModule : DIModule {

    override fun get(): Module = module {
        single<EngagementDatabase> { EngagementDatabase.getInstance(androidContext()) }
        factoryOf(::EngagementEntityDataMapperImpl) bind EngagementEntityDataMapper::class
        factory<LegacyEngagementDataMigrator> {
            LegacyEngagementDataMigratorImpl(
                sharedPreferences = androidContext().getSharedPreferences(
                    LegacyEngagementDataMigratorImpl.PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
                database = get(),
                engagementEntityDataMapper = get(),
                dispatcherProvider = get(),
            )
        }
        factoryOf(::EngagementDataLocalDataSourceImpl) bind EngagementDataLocalDataSource::class
        factoryOf(::EngagementDataRepositoryImpl) bind EngagementDataRepository::class
        factoryOf(::ObserveEngagementDataUseCaseImpl) bind ObserveEngagementDataUseCase::class
        factory<RateAppUseCase> {
            RateAppUseCaseImpl(
                inAppReview = get(),
                engagementDataRepository = get(),
            )
        }
        factoryOf(::MarkAppAsRatedUseCaseImpl) bind MarkAppAsRatedUseCase::class
        factory<ShareAppUseCase> {
            ShareAppUseCaseImpl(
                shareContentProvider = getOrNull() ?: DefaultShareContentProvider(androidContext()),
                inAppShare = get(),
            )
        }
        single<EngagementUseCase> {
            EngagementUseCaseImpl(
                rateAppUseCase = get(),
                shareAppUseCase = get(),
                engagementDataRepository = get(),
                engagementEligibility = getOrNull() ?: DefaultEngagementEligibility(),
            )
        }
    }
}
