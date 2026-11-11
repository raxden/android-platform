package com.raxdenstudios.platform.device.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.device.di.modules.DeviceDIDataModule

class DeviceDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        DeviceDIDataModule(),
    )
}
