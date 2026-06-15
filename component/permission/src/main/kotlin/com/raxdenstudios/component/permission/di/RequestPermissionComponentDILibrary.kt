package com.raxdenstudios.component.permission.di

import com.raxdenstudios.component.permission.di.modules.RequestPermissionComponentUIDIModule
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.core.di.FeatureDILibrary

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
