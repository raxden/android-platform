package com.raxdenstudios.component.permission.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.platform.core.util.IntentFactory
import com.raxdenstudios.platform.core.util.IntentType
import com.raxdenstudios.platform.ui.Component
import com.raxdenstudios.platform.ui.ScreenState
import com.raxdenstudios.platform.ui.composable.Dialog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RequestPermissionComponent(
    modifier: Modifier = Modifier,
    screenState: ScreenState,
    private val arguments: Arguments,
    onUIEvent: (RequestPermissionComponentUIEvent) -> Unit = {},
    viewModel: RequestPermissionComponentViewModel,
) : Component<RequestPermissionComponentUIState, RequestPermissionComponentUIEvent>(
    modifier = modifier,
    onUIEvent = onUIEvent,
    screenState = screenState,
    viewModel = viewModel
), KoinComponent, LifecycleEventObserver {

    private val intentFactory: IntentFactory by inject()

    data class Arguments(
        val permission: Permission,
    )

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event
    ) {
        if (event == Lifecycle.Event.ON_START) {
            onAction(RequestPermissionComponentAction.OnPermissionRequested(arguments.permission))
        }
    }

    @Composable
    override fun Content(uiState: RequestPermissionComponentUIState) {
        uiState.model ?: return

        Dialog.Simple(
            title = uiState.model.reason,
            message = uiState.model.reasonDescription,
            confirmButton = uiState.model.acceptLabel to {
                onAction(RequestPermissionComponentAction.OnRationaleAccepted)
            },
            dismissButton = uiState.model.deniedLabel to {
                onAction(RequestPermissionComponentAction.OnRationaleDismissed)
            }
        )
    }

    override suspend fun handleUIEvent(uiEvent: RequestPermissionComponentUIEvent) {
        when (uiEvent) {
            RequestPermissionComponentUIEvent.NavigateToSettings -> {
                val intent = intentFactory.build(IntentType.AppSettings)
                navigator.navigate(intent)
            }

            is RequestPermissionComponentUIEvent.Denied -> Unit
            is RequestPermissionComponentUIEvent.Granted -> Unit
        }
    }
}
