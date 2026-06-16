package com.raxdenstudios.platform.core.di

import android.content.Context

interface DIApp {

    fun initialize(context: Context)

    fun provideLibs(): List<DILibrary>
}
