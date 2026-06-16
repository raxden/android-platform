package com.raxdenstudios.platform.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.raxdenstudios.platform.ui.Action
import com.raxdenstudios.platform.ui.ScreenState
import com.raxdenstudios.platform.ui.UIState
import com.raxdenstudios.platform.ui.provider.ContentProvider
import com.raxdenstudios.platform.ui.theme.ThemeProvider

interface LayoutProvider<UIS : UIState> {

    val backgroundColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.background

    @Composable
    fun Apply(
        paddingValues: PaddingValues,
        screenState: ScreenState,
        uiState: UIS,
        onAction: (Action) -> Unit,
    )
}

class DefaultLayoutProvider<UIS : UIState>(
    private val themeProvider: ThemeProvider,
    private val dialogProvider: ContentProvider<UIS>? = null,
    private val contentProvider: ContentProvider<UIS>? = null,
) : LayoutProvider<UIS> {

    @Composable
    override fun Apply(
        paddingValues: PaddingValues,
        screenState: ScreenState,
        uiState: UIS,
        onAction: (Action) -> Unit,
    ) {
        themeProvider.Apply {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = backgroundColor
            ) {
                contentProvider?.Apply(paddingValues, screenState, uiState, onAction)
                dialogProvider?.Apply(paddingValues, screenState, uiState, onAction)
            }
        }
    }
}