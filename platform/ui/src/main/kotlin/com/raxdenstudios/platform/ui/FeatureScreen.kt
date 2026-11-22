package com.raxdenstudios.platform.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import com.raxdenstudios.commons.android.compose.ComposableLifecycle
import com.raxdenstudios.commons.android.compose.LockScreenOrientation
import com.raxdenstudios.commons.android.compose.ScreenOrientation
import com.raxdenstudios.platform.ui.layout.LayoutProvider
import com.raxdenstudios.platform.ui.navigation.DefaultNavigator
import com.raxdenstudios.platform.ui.navigation.Navigator

@Suppress("UNCHECKED_CAST")
class FeatureScreen<UIS, UIE>(
    private val paddingValues: PaddingValues = PaddingValues(),
    private val layoutProvider: LayoutProvider<UIS>,
    private val eventHandler: EventHandler<UIE>? = null,
    private val lifecycleEventObserver: LifecycleEventObserver? = null,
    private val backStackChangeHandler: BackStackChangeHandler? = null,
    var screenState: ScreenState,
    private val viewModel: ViewModel,
) where UIS : UIState,
          UIE : UIEvent {

    val navigator: Navigator
        get() = DefaultNavigator(
            screenState.navController,
        )

    val context: Context
        get() = screenState.navController.context

    val stateDelegate: StateDelegate<UIS>?
        get() = viewModel as? StateDelegate<UIS>

    val eventDelegate: EventDelegate<UIE>?
        get() = viewModel as? EventDelegate<UIE>

    val onAction: (Action) -> Unit
        get() = { action -> (viewModel as? ActionDelegate<Action>)?.onAction(action) }

    @Composable
    operator fun invoke() {
        LockScreenOrientation(orientation = screenState.orientation.toScreenOrientation())

        ObserveLifecycle()
        ObserveBackStackEntryState()
        ObserveUIEvent()
        ObserveUIState()
    }

    @Composable
    private fun ObserveLifecycle() {
        lifecycleEventObserver?.let { handler -> ComposableLifecycle(handler) }
    }

    @Composable
    private fun ObserveBackStackEntryState() {
        val navBackStackEntry by screenState.navController.currentBackStackEntryAsState()
        navBackStackEntry?.run { backStackChangeHandler?.onBackStackChange(this) }
    }

    @Composable
    private fun ObserveUIEvent() {
        LaunchedEffect(Unit) {
            val eventDelegate = eventDelegate ?: return@LaunchedEffect
            eventDelegate.viewEvent.collect { uiEvent ->
                eventHandler?.handle(uiEvent, screenState, navigator)
            }
        }
    }

    @Composable
    private fun ObserveUIState() {
        val stateDelegate = stateDelegate ?: return
        val uiState by stateDelegate.uiState.collectAsStateWithLifecycle()

        layoutProvider.Apply(paddingValues, screenState, uiState, onAction)
    }
}

private fun ScreenState.Orientation.toScreenOrientation(): ScreenOrientation = when (this) {
    ScreenState.Orientation.PORTRAIT -> ScreenOrientation.Portrait
    ScreenState.Orientation.LANDSCAPE -> ScreenOrientation.Landscape
}
