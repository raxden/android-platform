package com.raxdenstudios.platform.device.data.repository

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.permissions.PermissionsManager
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.platform.device.domain.model.PermissionResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PermissionRepositoryImplTest {

    private val permissionsManager: PermissionsManager = mockk()
    private val repository = PermissionRepositoryImpl(permissionsManager)

    @Test
    fun `hasPermission returns true when granted`() = runTest {
        every {
            permissionsManager.hasPermission(any(), Permission.Camera)
        } answers { firstArg<(Boolean) -> Unit>().invoke(true) }

        val result = repository.hasPermission(Permission.Camera)

        assertThat(result).isEqualTo(Answer.Success(true))
    }

    @Test
    fun `hasPermission returns false when denied`() = runTest {
        every {
            permissionsManager.hasPermission(any(), Permission.Camera)
        } answers { firstArg<(Boolean) -> Unit>().invoke(false) }

        val result = repository.hasPermission(Permission.Camera)

        assertThat(result).isEqualTo(Answer.Success(false))
    }

    @Test
    fun `requestPermission returns Granted`() = runTest {
        every {
            permissionsManager.requestPermission(any(), *anyVararg())
        } answers { firstArg<PermissionsManager.Callbacks>().onGranted(Permission.Camera) }

        val result = repository.requestPermission(Permission.Camera)

        assertThat(result).isEqualTo(Answer.Success(PermissionResult.Granted(Permission.Camera)))
    }

    @Test
    fun `requestPermission returns Denied`() = runTest {
        every {
            permissionsManager.requestPermission(any(), *anyVararg())
        } answers { firstArg<PermissionsManager.Callbacks>().onDenied(Permission.Camera) }

        val result = repository.requestPermission(Permission.Camera)

        assertThat(result).isEqualTo(Answer.Success(PermissionResult.Denied(Permission.Camera)))
    }

    @Test
    fun `requestPermission returns Rationale`() = runTest {
        every {
            permissionsManager.requestPermission(any(), *anyVararg())
        } answers { firstArg<PermissionsManager.Callbacks>().onRationale(Permission.Camera) }

        val result = repository.requestPermission(Permission.Camera)

        assertThat(result).isEqualTo(Answer.Success(PermissionResult.Rationale(Permission.Camera)))
    }
}
