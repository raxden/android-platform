package com.raxdenstudios.platform.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.navOptions

fun <T : Any> NavHostController.navigateToTopLevelDestination(
    destination: T,
) {
    val navOptions = navOptions {
        // Pop up to the start destination of the graph to avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
    navigate(destination)
}
