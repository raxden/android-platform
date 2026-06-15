package com.raxdenstudios.platform.rating.di.modules

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.rating.data.RatingLocalDataSource
import com.raxdenstudios.platform.rating.data.RatingLocalDataSourceImpl
import com.raxdenstudios.platform.rating.data.RatingRepository
import com.raxdenstudios.platform.rating.data.RatingRepositoryImpl
import com.raxdenstudios.platform.rating.domain.ShouldShowRatingDialogUseCase
import com.raxdenstudios.platform.rating.domain.ShouldShowRatingDialogUseCaseImpl
import com.raxdenstudios.platform.rating.domain.UpdateRatingStateUseCase
import com.raxdenstudios.platform.rating.domain.UpdateRatingStateUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class RatingComponentDIDataModule : DIModule {

    override fun get(): Module = module {
        singleOf(::RatingLocalDataSourceImpl) bind RatingLocalDataSource::class
        factoryOf(::RatingRepositoryImpl) bind RatingRepository::class
        factoryOf(::ShouldShowRatingDialogUseCaseImpl) bind ShouldShowRatingDialogUseCase::class
        factoryOf(::UpdateRatingStateUseCaseImpl) bind UpdateRatingStateUseCase::class
    }
}
