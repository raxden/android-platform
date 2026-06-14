package com.raxdenstudios.platform.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun rememberDefaultScreenState(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): ScreenState = remember(navController, snackbarHostState) {
    DefaultScreenState(
        navController = navController,
        snackbarHostState = snackbarHostState,
    )
}

interface ScreenState {

    val navController: NavHostController
    val snackbarHostState: SnackbarHostState
    val orientation: Orientation

    suspend fun showSnackBar(message: String) {
        snackbarHostState.showSnackbar(message)
    }

    enum class Orientation {
        PORTRAIT,
        LANDSCAPE
    }
}

class DefaultScreenState(
    override val navController: NavHostController,
    override val snackbarHostState: SnackbarHostState,
    override val orientation: ScreenState.Orientation = ScreenState.Orientation.PORTRAIT,
) : ScreenState
