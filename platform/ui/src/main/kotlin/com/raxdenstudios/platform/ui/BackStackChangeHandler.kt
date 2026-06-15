package com.raxdenstudios.platform.ui

import androidx.navigation.NavBackStackEntry

interface BackStackChangeHandler {

    fun onBackStackChange(entry: NavBackStackEntry)
}
