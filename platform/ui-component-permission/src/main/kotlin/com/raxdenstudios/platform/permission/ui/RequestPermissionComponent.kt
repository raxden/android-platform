package com.raxdenstudios.platform.permission.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    arguments: Arguments,
    viewModel: RequestPermissionComponentViewModel,
) : Component<RequestPermissionComponentUIState, RequestPermissionComponentUIEvent>(
    modifier = modifier,
    screenState = screenState,
    viewModel = viewModel
), KoinComponent {

    private val intentFactory: IntentFactory by inject()

    data class Arguments(
        val permission: Permission,
    )

    init {
        onAction(RequestPermissionComponentAction.OnPermissionRequested(arguments.permission))
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
        }
    }
}

