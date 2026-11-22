package com.raxdenstudios.platform.device.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.permissions.PermissionsManager
import com.raxdenstudios.commons.permissions.ext.hasPermission
import com.raxdenstudios.commons.permissions.ext.requestPermission
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.commons.permissions.model.PermissionResult
import com.raxdenstudios.platform.core.domain.model.Failure

interface PermissionRepository {
    suspend fun hasPermission(
        permission: Permission,
    ): Answer<Boolean, Failure>
    suspend fun requestPermission(
        permission: Permission,
    ): Answer<PermissionResult, Failure>
}

internal class PermissionRepositoryImpl(
    private val permissionsManager: PermissionsManager,
) : PermissionRepository {

    override suspend fun hasPermission(
        permission: Permission,
    ): Answer<Boolean, Failure> = Answer.Success(permissionsManager.hasPermission(permission))

    override suspend fun requestPermission(
        permission: Permission,
    ): Answer<PermissionResult, Failure> = Answer.Success(permissionsManager.requestPermission(permission))
}
