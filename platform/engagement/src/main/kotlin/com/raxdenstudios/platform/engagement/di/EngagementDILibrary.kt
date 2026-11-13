package com.raxdenstudios.platform.engagement.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.di.module.EngagementDIModule

class EngagementDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        EngagementDIModule(),
    )
}
