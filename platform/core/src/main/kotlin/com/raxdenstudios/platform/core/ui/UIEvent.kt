package com.raxdenstudios.platform.core.ui

import java.util.UUID

interface UIEvent {

    val id: String
        get() = UUID.randomUUID().toString()
}
