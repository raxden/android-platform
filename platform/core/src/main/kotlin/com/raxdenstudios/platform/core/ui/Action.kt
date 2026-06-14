package com.raxdenstudios.platform.core.ui

import java.util.UUID

interface Action {

    val id: String
        get() = UUID.randomUUID().toString()
}
