package com.raxdenstudios.platform.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raxdenstudios.platform.ui.composable.ComposableLifecycle
import com.raxdenstudios.platform.ui.navigation.DefaultNavigator
import com.raxdenstudios.platform.ui.navigation.Navigator

@Suppress("UNCHECKED_CAST")
abstract class Component<UIS, UIE>(
    private val modifier: Modifier = Modifier,
    private val onUIEvent: (UIE) -> Unit = {},
    val screenState: ScreenState,
    private val viewModel: ViewModel,
) where UIS : UIState,
        UIE : UIEvent {

    val navigator: Navigator
        get() = DefaultNavigator(
            screenState.navController
        )

    val context: Context
        get() = screenState.navController.context

    val stateDelegate: StateDelegate<UIS>?
        get() = viewModel as? StateDelegate<UIS>

    val eventDelegate: EventDelegate<UIE>?
        get() = viewModel as? EventDelegate<UIE>

    val onAction: (Action) -> Unit
        get() = { action -> (viewModel as? ActionDelegate<Action>)?.onAction(action) }

    val lifecycleEventObserver: LifecycleEventObserver?
        get() = this as? LifecycleEventObserver

    @Composable
    operator fun invoke() {
        ObserveLifecycle()
        ObserveUIState()
        ObserveUIEvent()
    }

    @Composable
    private fun ObserveLifecycle() {
        lifecycleEventObserver?.let { handler -> ComposableLifecycle(handler) }
    }

    @Composable
    private fun ObserveUIEvent() {
        LaunchedEffect(Unit) {
            val eventDelegate = eventDelegate ?: return@LaunchedEffect
            eventDelegate.viewEvent.collect { uiEvent ->
                handleUIEvent(uiEvent)
                onUIEvent.invoke(uiEvent)
            }
        }
    }

    @Composable
    private fun ObserveUIState() {
        val stateDelegate = stateDelegate ?: return
        val uiState by stateDelegate.uiState.collectAsStateWithLifecycle()

        Column(
            modifier = modifier
        ) {
            Content(uiState)
        }
    }

    @Composable
    abstract fun Content(uiState: UIS)

    abstract suspend fun handleUIEvent(uiEvent: UIE)
}
