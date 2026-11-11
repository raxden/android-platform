package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.repository.FCMRepository

interface GetFCMTokenUseCase : SuspendUseCase<GetFCMTokenUseCase.Params, String> {
    data object Params
}

class GetFCMTokenUseCaseImpl(private val fcmRepository: FCMRepository) : GetFCMTokenUseCase {

    override suspend fun invoke(params: GetFCMTokenUseCase.Params): Answer<String, Failure> = fcmRepository.getToken()
}
