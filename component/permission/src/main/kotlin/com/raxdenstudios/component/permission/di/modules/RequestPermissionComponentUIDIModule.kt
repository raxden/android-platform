package com.raxdenstudios.component.permission.di.modules

import com.raxdenstudios.component.permission.ui.RequestPermissionComponentEventDelegate
import com.raxdenstudios.component.permission.ui.RequestPermissionComponentStateDelegate
import com.raxdenstudios.component.permission.ui.RequestPermissionComponentStateDelegateImpl
import com.raxdenstudios.component.permission.ui.RequestPermissionComponentViewModel
import com.raxdenstudios.component.permission.ui.mapper.RequestPermissionComponentMapper
import com.raxdenstudios.component.permission.ui.mapper.RequestPermissionComponentMapperImpl
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.ui.EventDelegateImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

class RequestPermissionComponentUIDIModule : DIModule {

    override fun get(): Module = module {
        factoryOf(::RequestPermissionComponentMapperImpl) bind RequestPermissionComponentMapper::class

        factory<RequestPermissionComponentStateDelegate> { RequestPermissionComponentStateDelegateImpl() }
        factory<RequestPermissionComponentEventDelegate> { EventDelegateImpl() }
        viewModelOf(::RequestPermissionComponentViewModel)
    }
}
