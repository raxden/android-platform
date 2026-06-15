package com.raxdenstudios.component.permission.ui.mapper

import android.annotation.SuppressLint
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.component.permission.R
import com.raxdenstudios.component.permission.ui.model.RequestPermissionModel

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
            reason = stringProvider.getString(R.string.component_permission_notifications_title),
            reasonDescription = stringProvider.getString(R.string.component_permission_notifications_description),
            acceptLabel = stringProvider.getString(R.string.component_permission_notifications_accept),
            deniedLabel = stringProvider.getString(R.string.component_permission_notifications_denied),
        )

        Permission.ReadContacts -> TODO()
        Permission.ReadExternalStorage -> TODO()
        Permission.RecordAudio -> TODO()
        Permission.WriteContacts -> TODO()
        Permission.WriteExternalStorage -> TODO()
    }
}

