package com.raxdenstudios.platform.core.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.module.Module

abstract class KoinDIApp : DIApp {

    override fun initialize(context: Context) {
        provideLibs().forEach { it.initialize() }

        startKoin {
            androidContext(context)
            androidLogger()
            androidFileProperties()
            workManagerFactory()
            modules(provideLibs().toModules())
        }
    }

    private fun List<DILibrary>.toModules(): List<Module> =
        flatMap { library -> library.getModules().map { module -> module.get() } }
}
