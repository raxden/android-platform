package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.permissions.model.Permission
import com.raxdenstudios.platform.device.data.repository.PermissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class HasPermissionUseCaseImplTest {

    private val permissionRepository: PermissionRepository = mockk()
    private val useCase = HasPermissionUseCaseImpl(permissionRepository)

    @Test
    fun `invoke returns repository answer`() = runTest {
        coEvery { permissionRepository.hasPermission(Permission.Camera) } returns
            Answer.Success(true)

        val result = useCase(HasPermissionUseCase.Params(Permission.Camera))

        assertThat(result).isEqualTo(Answer.Success(true))
        coVerify { permissionRepository.hasPermission(Permission.Camera) }
    }
}
