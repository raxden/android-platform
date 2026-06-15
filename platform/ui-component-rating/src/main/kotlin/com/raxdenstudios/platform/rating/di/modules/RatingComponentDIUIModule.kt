package com.raxdenstudios.platform.rating.di.modules

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.ui.EventDelegateImpl
import com.raxdenstudios.platform.rating.ui.RatingComponentViewModel
import com.raxdenstudios.platform.rating.ui.RatingComponentEventDelegate
import com.raxdenstudios.platform.rating.ui.RatingComponentStateDelegate
import com.raxdenstudios.platform.rating.ui.RatingComponentStateDelegateImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class RatingComponentDIUIModule : DIModule {

    override fun get(): Module = module {
        factory<RatingComponentStateDelegate> { RatingComponentStateDelegateImpl() }
        factory<RatingComponentEventDelegate> { EventDelegateImpl() }
        viewModelOf(::RatingComponentViewModel)
    }
}
