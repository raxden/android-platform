package com.raxdenstudios.platform.rating.ui

import androidx.compose.runtime.Immutable
import com.raxdenstudios.platform.ui.Action
import com.raxdenstudios.platform.ui.UIEvent
import com.raxdenstudios.platform.ui.UIState

sealed class RatingComponentUIEvent : UIEvent {
    data object OpenPlayStore : RatingComponentUIEvent()
}

@Immutable
data class RatingComponentUIState(
    val isVisible: Boolean = false,
) : UIState()

sealed class RatingComponentAction : Action {
    data object RateApp : RatingComponentAction()
    data object DismissRating : RatingComponentAction()
}
