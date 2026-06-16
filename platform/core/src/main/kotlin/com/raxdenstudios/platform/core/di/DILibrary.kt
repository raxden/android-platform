package com.raxdenstudios.platform.core.di

interface DILibrary {

    fun initialize()

    fun getModules(): List<DIModule>
}
