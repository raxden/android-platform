package com.raxdenstudios.platform.engagement.di.module

import android.content.Context
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSource
import com.raxdenstudios.platform.engagement.data.datasource.local.EngagementDataLocalDataSourceImpl
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepository
import com.raxdenstudios.platform.engagement.data.repository.EngagementDataRepositoryImpl
import com.raxdenstudios.platform.engagement.domain.DefaultEngagementEligibility
import com.raxdenstudios.platform.engagement.domain.DefaultShareContentProvider
import com.raxdenstudios.platform.engagement.domain.EngagementUseCase
import com.raxdenstudios.platform.engagement.domain.EngagementUseCaseImpl
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
 * Persistence is self-contained: the module creates its own `platform_engagement`
 * `SharedPreferences` file and a private `Json`, so consumers bind neither.
 */
class EngagementDIModule : DIModule {

    override fun get(): Module = module {
        single<EngagementDataLocalDataSource> {
            EngagementDataLocalDataSourceImpl(
                androidContext().getSharedPreferences(
                    EngagementDataLocalDataSourceImpl.PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
                dispatcherProvider = get(),
            )
        }
        factoryOf(::EngagementDataRepositoryImpl) bind EngagementDataRepository::class
        factoryOf(::ObserveEngagementDataUseCaseImpl) bind ObserveEngagementDataUseCase::class
        factoryOf(::RateAppUseCaseImpl) bind RateAppUseCase::class
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
