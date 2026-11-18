package com.raxdenstudios.platform.core.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure

interface SuspendUseCase<in Params, out Data> {

    suspend operator fun invoke(
        params: Params,
    ): Answer<Data, Failure>
}
