package com.raxdenstudios.platform.ui.navigation

import android.content.Intent
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

interface Navigator {

    fun navigate(
        destination: Destination,
        navOptions: NavOptionsBuilder.() -> Unit = {}
    )

    fun navigate(
        intent: Intent,
    )

    fun navigateUp()

    fun navigateToTopLevelDestination(
        destination: Destination
    )
}

class DefaultNavigator(
    private val navController: NavHostController
): Navigator {

    override fun navigate(intent: Intent) {
        navController.context.startActivity(intent)
    }

    override fun navigate(destination: Destination, navOptions: NavOptionsBuilder.() -> Unit) {
        navController.navigate(destination, navOptions)
    }

    override fun navigateUp() {
        navController.navigateUp()
    }

    override fun navigateToTopLevelDestination(destination: Destination) {
        navController.navigateToTopLevelDestination(destination)
    }
}
