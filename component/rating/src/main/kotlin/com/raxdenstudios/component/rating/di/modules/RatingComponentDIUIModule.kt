package com.raxdenstudios.component.rating.di.modules

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.ui.EventDelegateImpl
import com.raxdenstudios.component.rating.ui.RatingComponentViewModel
import com.raxdenstudios.component.rating.ui.RatingComponentEventDelegate
import com.raxdenstudios.component.rating.ui.RatingComponentStateDelegate
import com.raxdenstudios.component.rating.ui.RatingComponentStateDelegateImpl
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
