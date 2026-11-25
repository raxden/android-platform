package com.raxdenstudios.platform.core.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Starts Koin with the modules of every library returned by [provideLibs], in list order.
 *
 * Koin's default `allowOverride = true` is kept: when two modules bind the same type and
 * qualifier, the one loaded last (its library listed later) silently wins. Apps rely on this to
 * replace a platform default on purpose, so never keep a copy of a binding a platform library
 * already provides (see `CoreDILibrary`).
 */
abstract class KoinDIApp : DIApp {

    override fun initialize(
        context: Context,
    ) {
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
