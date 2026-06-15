package com.raxdenstudios.platform.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raxdenstudios.platform.ui.composable.ComposableLifecycle
import com.raxdenstudios.platform.ui.navigation.DefaultNavigator
import com.raxdenstudios.platform.ui.navigation.Navigator

@Suppress("UNCHECKED_CAST")
abstract class Component<UIS, UIE>(
    private val modifier: Modifier = Modifier,
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

    @Composable
    operator fun invoke() {
        ComposableLifecycle(onEvent = ::onLifecycleEvent)

        ObserveUIState()
        ObserveUIEvent()
    }

    @Composable
    private fun ObserveUIEvent() {
        LaunchedEffect(Unit) { // Execute only once
            val eventDelegate = eventDelegate ?: return@LaunchedEffect
            eventDelegate.viewEvent.collect { uiEvent -> handleUIEvent(uiEvent) }
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

    open fun onLifecycleEvent(source: LifecycleOwner, event: Lifecycle.Event) {}
}
