package com.raxdenstudios.platform.core.di

import org.koin.core.module.Module

interface DIModule {

    fun get(): Module
}
