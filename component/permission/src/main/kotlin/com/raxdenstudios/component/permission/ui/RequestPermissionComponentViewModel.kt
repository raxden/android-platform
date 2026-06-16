package com.raxdenstudios.component.permission.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.raxdenstudios.commons.coroutines.ext.onCoSuccess
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.component.permission.ui.mapper.RequestPermissionComponentMapper
import com.raxdenstudios.platform.device.domain.model.PermissionResult
import com.raxdenstudios.platform.device.domain.usecase.RequestPermissionUseCase
import com.raxdenstudios.platform.ui.ActionDelegate
import com.raxdenstudios.platform.ui.EventDelegate
import com.raxdenstudios.platform.ui.safeLaunch

typealias RequestPermissionComponentEventDelegate = EventDelegate<RequestPermissionComponentUIEvent>
typealias RequestPermissionComponentActionDelegate = ActionDelegate<RequestPermissionComponentAction>

class RequestPermissionComponentViewModel(
    private val requestPermissionComponentMapper: RequestPermissionComponentMapper,
    private val requestPermissionUseCase: RequestPermissionUseCase,
    private val stateDelegate: RequestPermissionComponentStateDelegate,
    private val eventDelegate: RequestPermissionComponentEventDelegate,
) : ViewModel(), RequestPermissionComponentStateDelegate by stateDelegate,
    RequestPermissionComponentEventDelegate by eventDelegate,
    RequestPermissionComponentActionDelegate {

    override fun onAction(action: RequestPermissionComponentAction) {
        when (action) {
            RequestPermissionComponentAction.OnRationaleAccepted -> handleRationaleAccepted()
            RequestPermissionComponentAction.OnRationaleDismissed -> handleRationaleDismissed()
            is RequestPermissionComponentAction.OnPermissionRequested ->
                handlePermissionRequested(action.permission)
        }
    }

    private fun handlePermissionRequested(permission: Permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(permission)
        }
    }

    private fun handleRationaleAccepted() {
        emitEvent(RequestPermissionComponentUIEvent.NavigateToSettings)
        hideDialog()
    }

    private fun handleRationaleDismissed() {
        hideDialog()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission(permission: Permission) = safeLaunch {
        requestPermissionUseCase(RequestPermissionUseCase.Params(permission)).onCoSuccess { permissionResult ->
            when (permissionResult) {
                is PermissionResult.Denied -> handlePermissionDenied(permissionResult.permission)
                is PermissionResult.Granted -> handlePermissionGranted(permissionResult.permission)
                is PermissionResult.Rationale -> handlePermissionRationale(permissionResult.permission)
            }
        }
    }

    private fun handlePermissionDenied(permission: Permission) {
        emitEvent(RequestPermissionComponentUIEvent.Denied(permission))
    }

    private fun handlePermissionGranted(permission: Permission) {
        emitEvent(RequestPermissionComponentUIEvent.Granted(permission))
    }

    private fun handlePermissionRationale(permission: Permission) {
        val dialog = requestPermissionComponentMapper.toModel(permission)
        showDialog(dialog)
    }
}
