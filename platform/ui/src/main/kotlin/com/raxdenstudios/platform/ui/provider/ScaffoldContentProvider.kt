package com.raxdenstudios.platform.ui.provider

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.raxdenstudios.platform.ui.Action
import com.raxdenstudios.platform.ui.ScreenState
import com.raxdenstudios.platform.ui.UIState

class ScaffoldContentProvider<UIS : UIState>(
    private val topBarProvider: ContentProvider<UIS>? = null,
    private val bottomBarProvider: ContentProvider<UIS>? = null,
    private val contentProvider: ContentProvider<UIS>? = null,
) : ContentProvider<UIS>() {

    @Composable
    override fun Content(
        paddingValues: PaddingValues,
        screenState: ScreenState,
        uiState: UIS,
        onAction: (Action) -> Unit,
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val hasTopPadding = paddingValues.calculateTopPadding() > 0.dp
        val hasBottomPadding = paddingValues.calculateBottomPadding() > 0.dp
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // When coming from another Scaffold (top/bottom already applied),
        // subtract the system bar paddings
        val adjustedPadding = if (hasTopPadding || hasBottomPadding) {
            PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                top = (paddingValues.calculateTopPadding() - statusBarPadding).coerceAtLeast(0.dp),
                bottom = (paddingValues.calculateBottomPadding() - navigationBarPadding).coerceAtLeast(0.dp)
            )
        } else {
            paddingValues
        }

        Scaffold(
            modifier = Modifier.padding(adjustedPadding),
            snackbarHost = { SnackbarHost(screenState.snackbarHostState) },
            topBar = { topBarProvider?.Apply(paddingValues, screenState, uiState, onAction) },
            bottomBar = { bottomBarProvider?.Apply(paddingValues, screenState, uiState, onAction) },
        ) { pValues ->
            contentProvider?.Apply(pValues, screenState, uiState, onAction)
        }
    }
}