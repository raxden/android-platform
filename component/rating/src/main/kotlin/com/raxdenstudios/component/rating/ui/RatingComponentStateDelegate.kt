package com.raxdenstudios.component.rating.ui

import com.raxdenstudios.platform.ui.StateDelegate
import com.raxdenstudios.platform.ui.StateDelegateImpl

interface RatingComponentStateDelegate : StateDelegate<RatingComponentUIState> {
    fun showRating()
    fun hideRating()
}

class RatingComponentStateDelegateImpl : StateDelegateImpl<RatingComponentUIState>(),
    RatingComponentStateDelegate {

    override val initialUIState: RatingComponentUIState
        get() = RatingComponentUIState()

    override fun showRating() {
        updateState { value ->
            value.copy(isVisible = true)
        }
    }

    override fun hideRating() {
        updateState { value ->
            value.copy(isVisible = false)
        }
    }
}
