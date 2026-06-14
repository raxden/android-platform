package com.raxdenstudios.platform.core.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import com.raxdenstudios.platform.core.ui.ScreenState

interface GraphProvider {

    val startDestination: Destination

    val graph: Graph

    fun get(
        navGraphBuilder: NavGraphBuilder,
        paddingValues: PaddingValues,
        screenState: ScreenState,
    )
}
