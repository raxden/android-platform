package com.raxdenstudios.platform.core.domain

import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import kotlinx.coroutines.flow.Flow

interface FlowUseCase<in Params, out Data> {

    operator fun invoke(params: Params): Flow<Answer<Data, Failure>>
}
