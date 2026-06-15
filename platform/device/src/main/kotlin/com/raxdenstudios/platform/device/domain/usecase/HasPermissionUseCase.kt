package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.platform.device.data.repository.PermissionRepository
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.permissions.model.Permission

interface HasPermissionUseCase : SuspendUseCase<HasPermissionUseCase.Params, Boolean> {

    data class Params(
        val permission: Permission,
    )
}

internal class HasPermissionUseCaseImpl(
    private val permissionRepository: PermissionRepository,
) : HasPermissionUseCase {

    override suspend fun invoke(
        params: HasPermissionUseCase.Params,
    ): Answer<Boolean, Failure> =
        permissionRepository.hasPermission(params.permission)
}
