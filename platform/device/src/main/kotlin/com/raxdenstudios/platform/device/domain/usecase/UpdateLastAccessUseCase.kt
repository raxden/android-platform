package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.AppRepository

interface UpdateLastAccessUseCase : SuspendUseCase<UpdateLastAccessUseCase.Params, Unit> {

    data object Params
}

internal class UpdateLastAccessUseCaseImpl(
    private val appRepository: AppRepository,
) : UpdateLastAccessUseCase {

    override suspend fun invoke(
        params: UpdateLastAccessUseCase.Params,
    ): Answer<Unit, Failure> =
        appRepository.saveLastAccess(System.currentTimeMillis())
}
