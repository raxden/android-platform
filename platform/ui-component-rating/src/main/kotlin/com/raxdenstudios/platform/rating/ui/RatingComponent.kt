package com.raxdenstudios.platform.rating.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.raxdenstudios.platform.ui.Component
import com.raxdenstudios.platform.ui.ScreenState
import com.raxdenstudios.platform.core.util.IntentFactory
import com.raxdenstudios.platform.core.util.IntentType
import com.raxdenstudios.platform.ui.component.rating.R
import com.raxdenstudios.platform.ui.composable.Dialog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RatingComponent(
    modifier: Modifier = Modifier,
    screenState: ScreenState,
    viewModel: RatingComponentViewModel,
) : Component<RatingComponentUIState, RatingComponentUIEvent>(
    modifier = modifier,
    screenState = screenState,
    viewModel = viewModel
), KoinComponent {

    private val intentFactory: IntentFactory by inject()

    @Composable
    override fun Content(uiState: RatingComponentUIState) {
        if (uiState.isVisible) {
            Dialog.Simple(
                title = stringResource(R.string.rating_dialog_title),
                message = stringResource(R.string.rating_dialog_message),
                confirmButton = stringResource(R.string.rating_dialog_rate) to {
                    onAction(RatingComponentAction.RateApp)
                },
                dismissButton = stringResource(R.string.rating_dialog_later) to {
                    onAction(RatingComponentAction.DismissRating)
                },
                onDismissRequest = { onAction(RatingComponentAction.DismissRating) },
            )
        }
    }

    override suspend fun handleUIEvent(uiEvent: RatingComponentUIEvent) {
        when (uiEvent) {
            is RatingComponentUIEvent.OpenPlayStore -> {
                val intent = intentFactory.build(IntentType.PlayStore)
                navigator.navigate(intent)
            }
        }
    }
}

