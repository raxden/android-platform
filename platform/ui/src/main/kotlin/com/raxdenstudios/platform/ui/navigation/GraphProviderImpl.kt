package com.raxdenstudios.platform.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.raxdenstudios.platform.ui.ScreenState

abstract class GraphProviderImpl : GraphProvider {

    abstract fun NavGraphBuilder.content(
        paddingValues: PaddingValues,
        screenState: ScreenState,
    )

    override fun get(
        navGraphBuilder: NavGraphBuilder,
        paddingValues: PaddingValues,
        screenState: ScreenState,
    ) {
        navGraphBuilder.navigation(
            route = graph::class,
            startDestination = startDestination::class
        ) {
            content(paddingValues, screenState)
        }
    }
}
