package com.raxdenstudios.component.permission.ui

import com.raxdenstudios.component.permission.ui.model.RequestPermissionModel
import com.raxdenstudios.platform.ui.StateDelegate
import com.raxdenstudios.platform.ui.StateDelegateImpl

interface RequestPermissionComponentStateDelegate :
    StateDelegate<RequestPermissionComponentUIState> {
    fun showDialog(model: RequestPermissionModel)
    fun hideDialog()
}

class RequestPermissionComponentStateDelegateImpl :
    StateDelegateImpl<RequestPermissionComponentUIState>(),
    RequestPermissionComponentStateDelegate {

    override val initialUIState: RequestPermissionComponentUIState
        get() = RequestPermissionComponentUIState()

    override fun showDialog(model: RequestPermissionModel) {
        updateState { value ->
            value.copy(model = model)
        }
    }

    override fun hideDialog() {
        updateState { value ->
            value.copy(model = null)
        }
    }
}
