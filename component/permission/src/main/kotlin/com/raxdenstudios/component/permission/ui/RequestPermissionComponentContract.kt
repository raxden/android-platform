package com.raxdenstudios.component.permission.ui

import androidx.compose.runtime.Immutable
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.component.permission.ui.model.RequestPermissionModel
import com.raxdenstudios.platform.ui.Action
import com.raxdenstudios.platform.ui.UIEvent
import com.raxdenstudios.platform.ui.UIState

sealed class RequestPermissionComponentUIEvent : UIEvent {
    data object NavigateToSettings : RequestPermissionComponentUIEvent()
}

@Immutable
data class RequestPermissionComponentUIState(
    val model: RequestPermissionModel? = null,
) : UIState()

sealed class RequestPermissionComponentAction : Action {
    data class OnPermissionRequested(val permission: Permission) :
        RequestPermissionComponentAction()

    data object OnRationaleAccepted : RequestPermissionComponentAction()
    data object OnRationaleDismissed : RequestPermissionComponentAction()
}
