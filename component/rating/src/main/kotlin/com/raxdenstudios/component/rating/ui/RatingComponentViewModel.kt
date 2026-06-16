package com.raxdenstudios.component.rating.ui

import androidx.lifecycle.ViewModel
import com.raxdenstudios.commons.core.ext.onSuccess
import com.raxdenstudios.platform.ui.ActionDelegate
import com.raxdenstudios.platform.ui.EventDelegate
import com.raxdenstudios.component.rating.domain.ShouldShowRatingDialogUseCase
import com.raxdenstudios.component.rating.domain.UpdateRatingStateUseCase
import com.raxdenstudios.platform.ui.safeLaunch

typealias RatingComponentEventDelegate = EventDelegate<RatingComponentUIEvent>
typealias RatingComponentActionDelegate = ActionDelegate<RatingComponentAction>

class RatingComponentViewModel(
    private val shouldShowRatingDialogUseCase: ShouldShowRatingDialogUseCase,
    private val updateRatingStateUseCase: UpdateRatingStateUseCase,
    private val stateDelegate: RatingComponentStateDelegate,
    private val eventDelegate: RatingComponentEventDelegate,
) : ViewModel(),
    RatingComponentStateDelegate by stateDelegate,
    RatingComponentEventDelegate by eventDelegate,
    RatingComponentActionDelegate {

    init {
        checkRatingDialog()
    }

    override fun onAction(action: RatingComponentAction) {
        when (action) {
            is RatingComponentAction.RateApp -> rateApp()
            is RatingComponentAction.DismissRating -> dismissRating()
        }
    }

    private fun checkRatingDialog() = safeLaunch {
        shouldShowRatingDialogUseCase()
            .onSuccess { shouldShow ->
                if (shouldShow) {
                    showRating()
                }
            }
    }

    private fun rateApp() = safeLaunch {
        updateRatingStateUseCase.markAsRated()
            .onSuccess {
                hideRating()
                emitEvent(RatingComponentUIEvent.OpenPlayStore)
            }
    }

    private fun dismissRating() = safeLaunch {
        updateRatingStateUseCase.markAsDismissed()
            .onSuccess {
                hideRating()
            }
    }
}
