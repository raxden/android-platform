package com.raxdenstudios.platform.rating.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.rating.di.modules.RatingComponentDIDataModule
import com.raxdenstudios.platform.rating.di.modules.RatingComponentDIUIModule

class RatingComponentDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        RatingComponentDIDataModule(),
        RatingComponentDIUIModule(),
    )
}
