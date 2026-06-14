package com.raxdenstudios.platform.core.ui.di.modules

import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.core.ui.mapper.CurrencyModelMapper
import com.raxdenstudios.platform.core.ui.mapper.CurrencyModelMapperImpl
import com.raxdenstudios.platform.core.ui.mapper.DateDataMapper
import com.raxdenstudios.platform.core.ui.mapper.DateDataMapperImpl
import com.raxdenstudios.platform.core.ui.mapper.DurationModelMapper
import com.raxdenstudios.platform.core.ui.mapper.DurationModelMapperImpl
import com.raxdenstudios.platform.core.ui.mapper.ErrorModelDataMapper
import com.raxdenstudios.platform.core.ui.mapper.ErrorModelDataMapperImpl
import com.raxdenstudios.platform.core.ui.mapper.LanguageModelMapper
import com.raxdenstudios.platform.core.ui.mapper.LanguageModelMapperImpl
import com.raxdenstudios.platform.core.ui.theme.DefaultThemeProvider
import com.raxdenstudios.platform.core.ui.theme.ThemeManager
import com.raxdenstudios.platform.core.ui.theme.ThemeManagerImpl
import com.raxdenstudios.platform.core.ui.theme.ThemeProvider
import com.raxdenstudios.platform.core.util.IntentFactory
import com.raxdenstudios.platform.core.util.IntentFactoryImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class UIDIModule : DIModule {

    override fun get(): Module = module {
        factoryOf(::StringProvider)
        factoryOf(::IntentFactoryImpl) bind IntentFactory::class
        singleOf(::DefaultThemeProvider) bind ThemeProvider::class
        factoryOf(::ThemeManagerImpl) bind ThemeManager::class

        factoryOf(::CurrencyModelMapperImpl) bind CurrencyModelMapper::class
        factoryOf(::DateDataMapperImpl) bind DateDataMapper::class
        factoryOf(::DurationModelMapperImpl) bind DurationModelMapper::class
        factoryOf(::ErrorModelDataMapperImpl) bind ErrorModelDataMapper::class
        factoryOf(::LanguageModelMapperImpl) bind LanguageModelMapper::class
    }
}
