package com.raxdenstudios.platform.core.domain

import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer

interface SuspendUseCase<in Params, out Data> {

    suspend operator fun invoke(params: Params): Answer<Data, Failure>
}
