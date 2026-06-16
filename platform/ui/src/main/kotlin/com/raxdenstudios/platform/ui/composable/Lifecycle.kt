package com.raxdenstudios.platform.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ComposableLifecycle(
    observer: LifecycleEventObserver,
    lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    DisposableEffect(lifeCycleOwner) {
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
