package com.raxdenstudios.platform.device.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.permissions.PermissionsManager
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.model.PermissionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface PermissionRepository {
    suspend fun hasPermission(permission: Permission): Answer<Boolean, Failure>
    suspend fun requestPermission(permission: Permission): Answer<PermissionResult, Failure>
}

internal class PermissionRepositoryImpl(
    private val permissionsManager: PermissionsManager,
) : PermissionRepository {

    override suspend fun hasPermission(permission: Permission): Answer<Boolean, Failure> =
        suspendCancellableCoroutine { continuation ->
            permissionsManager.hasPermission({ onGranted ->
                continuation.resume(Answer.Success(onGranted))
            }, permission)
        }

    override suspend fun requestPermission(permission: Permission): Answer<PermissionResult, Failure> =
        suspendCancellableCoroutine { continuation ->
            permissionsManager.requestPermission(
                permissions = arrayOf(permission),
                callbacks = PermissionsManager.Callbacks(
                    onGranted = {
                        val answer = Answer.Success(PermissionResult.Granted(permission))
                        continuation.resume(answer)
                    },
                    onRationale = {
                        val answer = Answer.Success(PermissionResult.Rationale(permission))
                        continuation.resume(answer)
                    },
                    onDenied = {
                        val answer = Answer.Success(PermissionResult.Denied(permission))
                        continuation.resume(answer)
                    },
                )
            )
        }
}
