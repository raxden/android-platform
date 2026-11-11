package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.repository.FCMRepository

interface SaveFCMTokenUseCase : SuspendUseCase<SaveFCMTokenUseCase.Params, Unit> {
    data class Params(val token: String)
}

class SaveFCMTokenUseCaseImpl(private val fcmRepository: FCMRepository) : SaveFCMTokenUseCase {

    override suspend fun invoke(params: SaveFCMTokenUseCase.Params): Answer<Unit, Failure> =
        fcmRepository.saveToken(params.token)
}
