package com.raxdenstudios.platform.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ComposableLifecycle(
    lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
) {
    ComposableLifecycle2(
        lifeCycleOwner = lifeCycleOwner,
        observer = LifecycleEventObserver { source, event ->
            onEvent(source, event)
        }
    )
}

@Composable
fun ComposableLifecycle2(
    lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    observer: LifecycleObserver,
) {
    DisposableEffect(lifeCycleOwner) {
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
