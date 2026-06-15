package com.raxdenstudios.platform.permission.di.modules

import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.permission.ui.RequestPermissionComponentEventDelegate
import com.raxdenstudios.platform.permission.ui.RequestPermissionComponentStateDelegate
import com.raxdenstudios.platform.permission.ui.RequestPermissionComponentStateDelegateImpl
import com.raxdenstudios.platform.permission.ui.RequestPermissionComponentViewModel
import com.raxdenstudios.platform.ui.EventDelegateImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class RequestPermissionComponentUIDIModule : DIModule {

    override fun get(): Module = module {
        factory<RequestPermissionComponentStateDelegate> { RequestPermissionComponentStateDelegateImpl() }
        factory<RequestPermissionComponentEventDelegate> { EventDelegateImpl() }
        viewModelOf(::RequestPermissionComponentViewModel)
    }
}
