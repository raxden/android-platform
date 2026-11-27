package com.raxdenstudios.platform.featureflag.di.module

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.featureflag.domain.FetchFeatureFlagsUseCase
import com.raxdenstudios.platform.featureflag.domain.FetchFeatureFlagsUseCaseImpl
import com.raxdenstudios.platform.featureflag.domain.IsFeatureFlagEnabledUseCase
import com.raxdenstudios.platform.featureflag.domain.IsFeatureFlagEnabledUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal class FeatureFlagDomainDIModule : DIModule {

    override fun get(): Module = module {
        factoryOf(::FetchFeatureFlagsUseCaseImpl) bind FetchFeatureFlagsUseCase::class
        factoryOf(::IsFeatureFlagEnabledUseCaseImpl) bind IsFeatureFlagEnabledUseCase::class
    }
}
