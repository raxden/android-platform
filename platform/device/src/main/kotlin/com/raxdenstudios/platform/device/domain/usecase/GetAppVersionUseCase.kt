package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.device.data.repository.AppRepository

interface GetAppVersionUseCase : SuspendUseCase<GetAppVersionUseCase.Params, String> {

    data object Params
}

class GetAppVersionUseCaseImpl(
    private val settingsRepository: AppRepository
) : GetAppVersionUseCase {

    override suspend fun invoke(params: GetAppVersionUseCase.Params): Answer<String, Failure> =
        settingsRepository.getAppVersion()
}
