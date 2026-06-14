package com.raxdenstudios.platform.core.ui

import com.raxdenstudios.platform.core.ui.navigation.Navigator

interface EventHandler<UIE : UIEvent> {

    suspend fun handle(
        uiEvent: UIE,
        screenState: ScreenState,
        navigator: Navigator,
    )
}
