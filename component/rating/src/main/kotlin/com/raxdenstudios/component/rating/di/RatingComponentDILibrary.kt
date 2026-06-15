package com.raxdenstudios.component.rating.di

import com.raxdenstudios.component.rating.di.modules.RatingComponentDIDataModule
import com.raxdenstudios.component.rating.di.modules.RatingComponentDIUIModule
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.core.di.FeatureDILibrary

class RatingComponentDILibrary : FeatureDILibrary() {

    override fun initialize() {
        // empty block
    }

    override fun getDataModules(): List<DIModule> = listOf(
        RatingComponentDIDataModule()
    )

    override fun getUIModules(): List<DIModule> = listOf(
        RatingComponentDIUIModule()
    )
}
