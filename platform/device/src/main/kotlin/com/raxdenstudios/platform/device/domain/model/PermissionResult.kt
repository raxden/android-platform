package com.raxdenstudios.platform.device.domain.model

import com.raxdenstudios.commons.permissions.model.Permission

sealed class PermissionResult {

    abstract val permission: Permission

    data class Granted(override val permission: Permission) : PermissionResult()
    data class Denied(override val permission: Permission) : PermissionResult()
    data class Rationale(override val permission: Permission) : PermissionResult()
}
