package com.raxdenstudios.platform.network.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.network.di.modules.NetworkDIModule

class NetworkDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        NetworkDIModule()
    )
}
