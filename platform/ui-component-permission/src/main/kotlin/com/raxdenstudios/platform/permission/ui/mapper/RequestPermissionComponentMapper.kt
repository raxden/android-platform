package com.raxdenstudios.platform.permission.ui.mapper

import android.annotation.SuppressLint
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.platform.permission.ui.model.RequestPermissionModel
import com.raxdenstudios.platform.ui.component.permission.R

interface RequestPermissionComponentMapper {

    fun toModel(permission: Permission): RequestPermissionModel
}

class RequestPermissionComponentMapperImpl(
    private val stringProvider: StringProvider,
) : RequestPermissionComponentMapper {

    @SuppressLint("NewApi")
    override fun toModel(permission: Permission) = when (permission) {
        Permission.AccessCoarseLocation -> TODO()
        Permission.AccessFineLocation -> TODO()
        Permission.CallPhone -> TODO()
        Permission.Camera -> TODO()
        is Permission.Other -> TODO()
        Permission.PostNotifications -> RequestPermissionModel(
            reason = stringProvider.getString(R.string.notifications_permission_title),
            reasonDescription = stringProvider.getString(R.string.notifications_permission_description),
            acceptLabel = stringProvider.getString(R.string.notifications_permission_accept),
            deniedLabel = stringProvider.getString(R.string.notifications_permission_denied),
        )

        Permission.ReadContacts -> TODO()
        Permission.ReadExternalStorage -> TODO()
        Permission.RecordAudio -> TODO()
        Permission.WriteContacts -> TODO()
        Permission.WriteExternalStorage -> TODO()
    }
}

