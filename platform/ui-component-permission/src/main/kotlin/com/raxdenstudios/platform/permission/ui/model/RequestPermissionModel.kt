package com.raxdenstudios.platform.permission.ui.model

import androidx.compose.runtime.Immutable

@Immutable
class RequestPermissionModel(
    val reason: String,
    val reasonDescription: String,
    val acceptLabel: String,
    val deniedLabel: String,
)
