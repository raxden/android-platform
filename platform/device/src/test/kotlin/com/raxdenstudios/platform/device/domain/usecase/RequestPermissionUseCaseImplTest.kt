package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.PermissionRepository
import com.raxdenstudios.platform.device.domain.model.PermissionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class RequestPermissionUseCaseImplTest {

    private val permissionRepository: PermissionRepository = mockk()
    private val useCase = RequestPermissionUseCaseImpl(permissionRepository)

    @Test
    fun `invoke returns Granted without requesting when already granted`() = runTest {
        coEvery { permissionRepository.hasPermission(Permission.Camera) } returns
            Answer.Success(true)

        val result = useCase(RequestPermissionUseCase.Params(Permission.Camera))

        assertThat(result).isEqualTo(Answer.Success(PermissionResult.Granted(Permission.Camera)))
        coVerify(exactly = 0) { permissionRepository.requestPermission(any()) }
    }

    @Test
    fun `invoke requests permission when not granted`() = runTest {
        coEvery { permissionRepository.hasPermission(Permission.Camera) } returns
            Answer.Success(false)
        coEvery { permissionRepository.requestPermission(Permission.Camera) } returns
            Answer.Success(PermissionResult.Granted(Permission.Camera))

        val result = useCase(RequestPermissionUseCase.Params(Permission.Camera))

        assertThat(result).isEqualTo(Answer.Success(PermissionResult.Granted(Permission.Camera)))
        coVerify { permissionRepository.requestPermission(Permission.Camera) }
    }

    @Test
    fun `invoke propagates hasPermission failure`() = runTest {
        val failure = Failure.Unknown("error")
        coEvery { permissionRepository.hasPermission(Permission.Camera) } returns
            Answer.Failure(failure)

        val result = useCase(RequestPermissionUseCase.Params(Permission.Camera))

        assertThat(result).isEqualTo(Answer.Failure(failure))
        coVerify(exactly = 0) { permissionRepository.requestPermission(any()) }
    }
}
