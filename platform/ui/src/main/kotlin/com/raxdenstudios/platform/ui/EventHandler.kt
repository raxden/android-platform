package com.raxdenstudios.platform.ui

import com.raxdenstudios.platform.ui.navigation.Navigator

interface EventHandler<UIE : UIEvent> {

    suspend fun handle(
        uiEvent: UIE,
        screenState: ScreenState,
        navigator: Navigator,
    )
}
