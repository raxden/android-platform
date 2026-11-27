package com.raxdenstudios.platform.featureflag.di.module

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.featureflag.data.datasource.remote.FeatureFlagRemoteDataSource
import com.raxdenstudios.platform.featureflag.data.datasource.remote.FeatureFlagRemoteDataSourceImpl
import com.raxdenstudios.platform.featureflag.data.repository.FeatureFlagRepository
import com.raxdenstudios.platform.featureflag.data.repository.FeatureFlagRepositoryImpl
import com.raxdenstudios.platform.featureflag.domain.model.FeatureFlag
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Duration

/**
 * The data source is a single: it holds the first activation gate every read waits on.
 * `FirebaseRemoteConfig` is not bound in Koin, so the library never clashes with an app binding of it.
 */
internal class FeatureFlagDataDIModule(
    private val flags: List<FeatureFlag>,
    private val minimumFetchInterval: Duration,
) : DIModule {

    override fun get(): Module = module {
        single<FeatureFlagRemoteDataSource> {
            FeatureFlagRemoteDataSourceImpl(
                remoteConfig = FirebaseRemoteConfig.getInstance(),
                flags = flags,
                minimumFetchInterval = minimumFetchInterval,
            )
        }
        factoryOf(::FeatureFlagRepositoryImpl) bind FeatureFlagRepository::class
    }
}
