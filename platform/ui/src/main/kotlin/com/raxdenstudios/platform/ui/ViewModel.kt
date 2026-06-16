package com.raxdenstudios.platform.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raxdenstudios.commons.coroutines.ext.launch
import com.raxdenstudios.commons.coroutines.ext.safeLaunch
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

interface StateDelegate<UIS : UIState> {
    val uiState: StateFlow<UIS>

    fun updateState(function: (UIS) -> UIS)
}

interface EventDelegate<UIE : UIEvent> {
    val viewEvent: Flow<UIE>

    fun ViewModel.emitEvent(newEvent: UIE)
}

interface ActionDelegate<UE : Action> {
    fun onAction(action: UE)
}

abstract class StateDelegateImpl<UIS : UIState> : StateDelegate<UIS> {

    abstract val initialUIState: UIS
    private val _uiState: MutableStateFlow<UIS> by lazy { MutableStateFlow(initialUIState) }
    override val uiState: StateFlow<UIS> = _uiState.asStateFlow()

    override fun updateState(function: (UIS) -> UIS) {
        _uiState.update { function(it) }
    }
}

class EventDelegateImpl<UIE : UIEvent> : EventDelegate<UIE> {

    private val _viewEvent = Channel<UIE>(capacity = Channel.BUFFERED)
    override val viewEvent: Flow<UIE> = _viewEvent.receiveAsFlow()

    override fun ViewModel.emitEvent(newEvent: UIE) {
        viewModelScope.safeLaunch { _viewEvent.send(newEvent) }
    }
}

fun ViewModel.safeLaunch(block: suspend () -> Unit): Job {
    return viewModelScope.safeLaunch { block() }
}

fun ViewModel.launch(onError: (throwable: Throwable) -> Unit, block: suspend () -> Unit) {
    viewModelScope.launch(onError = onError) { block() }
}
