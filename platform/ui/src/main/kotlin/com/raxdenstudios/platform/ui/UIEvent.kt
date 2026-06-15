package com.raxdenstudios.platform.ui

import java.util.UUID

interface UIEvent {

    val id: String
        get() = UUID.randomUUID().toString()
}
