package com.raxdenstudios.platform.core.ui.provider

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.raxdenstudios.platform.core.ui.Action
import com.raxdenstudios.platform.core.ui.ScreenState
import com.raxdenstudios.platform.core.ui.UIState

abstract class ContentProvider<UIS : UIState> {

    lateinit var screenState: ScreenState
    var onAction: (Action) -> Unit = {}

    @Composable
    fun Apply(
        paddingValues: PaddingValues,
        screenState: ScreenState,
        uiState: UIS,
        onAction: (Action) -> Unit,
    ) {
        Content(paddingValues, screenState, uiState, onAction)
    }

    @Composable
    protected abstract fun Content(
        paddingValues: PaddingValues,
        screenState: ScreenState,
        uiState: UIS,
        onAction: (Action) -> Unit,
    )
}
