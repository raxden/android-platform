package com.raxdenstudios.platform.permission.di

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.core.di.FeatureDILibrary
import com.raxdenstudios.platform.permission.di.modules.RequestPermissionComponentUIDIModule

class RequestPermissionComponentDILibrary : FeatureDILibrary() {

    override fun initialize() {
        // empty block
    }

    override fun getDataModules(): List<DIModule> = listOf(
    )

    override fun getUIModules(): List<DIModule> = listOf(
        RequestPermissionComponentUIDIModule()
    )
}
