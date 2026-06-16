package com.raxdenstudios.platform.ui.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.ui.di.modules.UIDIModule

class UIDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        UIDIModule()
    )
}
