package com.raxdenstudios.platform.ui.di.modules

import com.raxdenstudios.commons.android.intent.IntentFactory
import com.raxdenstudios.commons.android.intent.IntentFactoryImpl
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.ui.mapper.DateDataMapper
import com.raxdenstudios.platform.ui.mapper.DateDataMapperImpl
import com.raxdenstudios.platform.ui.mapper.DurationModelMapper
import com.raxdenstudios.platform.ui.mapper.DurationModelMapperImpl
import com.raxdenstudios.platform.ui.mapper.ErrorModelDataMapper
import com.raxdenstudios.platform.ui.mapper.ErrorModelDataMapperImpl
import com.raxdenstudios.platform.ui.theme.DefaultThemeProvider
import com.raxdenstudios.platform.ui.theme.ThemeDelegate
import com.raxdenstudios.platform.ui.theme.ThemeDelegateImpl
import com.raxdenstudios.platform.ui.theme.ThemeManager
import com.raxdenstudios.platform.ui.theme.ThemeManagerImpl
import com.raxdenstudios.platform.ui.theme.ThemeProvider
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
        // Resolving ThemeDelegate needs the app to bind a ThemeSource.
        factoryOf(::ThemeDelegateImpl) bind ThemeDelegate::class

        factoryOf(::DateDataMapperImpl) bind DateDataMapper::class
        factoryOf(::DurationModelMapperImpl) bind DurationModelMapper::class
        factoryOf(::ErrorModelDataMapperImpl) bind ErrorModelDataMapper::class
    }
}
