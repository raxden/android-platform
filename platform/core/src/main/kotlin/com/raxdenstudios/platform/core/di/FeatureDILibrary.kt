package com.raxdenstudios.platform.core.di

abstract class FeatureDILibrary: DILibrary {

    override fun getModules(): List<DIModule> =
        getDataModules() + getUIModules()

    abstract fun getDataModules(): List<DIModule>

    abstract fun getUIModules(): List<DIModule>
}
