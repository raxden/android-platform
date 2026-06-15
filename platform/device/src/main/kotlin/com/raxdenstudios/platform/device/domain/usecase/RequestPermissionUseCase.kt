package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.platform.device.data.repository.PermissionRepository
import com.raxdenstudios.platform.device.domain.model.PermissionResult
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.commons.permissions.model.Permission

interface RequestPermissionUseCase :
    SuspendUseCase<RequestPermissionUseCase.Params, PermissionResult> {

    data class Params(
        val permission: Permission,
    )
}

internal class RequestPermissionUseCaseImpl(
    private val permissionRepository: PermissionRepository,
) : RequestPermissionUseCase {

    override suspend fun invoke(
        params: RequestPermissionUseCase.Params,
    ): Answer<PermissionResult, Failure> =
        permissionRepository.hasPermission(params.permission)
            .coFlatMap { hasPermission ->
                if (hasPermission) {
                    Answer.Success(PermissionResult.Granted(params.permission))
                } else {
                    permissionRepository.requestPermission(params.permission)
                }
            }
}
