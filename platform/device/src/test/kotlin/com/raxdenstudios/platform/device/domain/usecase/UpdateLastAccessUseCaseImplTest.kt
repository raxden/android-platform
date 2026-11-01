package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.device.data.repository.AppRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class UpdateLastAccessUseCaseImplTest {

    private val appRepository: AppRepository = mockk()
    private val useCase = UpdateLastAccessUseCaseImpl(appRepository)

    @Test
    fun `invoke saves current timestamp in repository`() = runTest {
        coEvery { appRepository.saveLastAccess(any()) } returns Answer.Success(Unit)

        val result = useCase(UpdateLastAccessUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { appRepository.saveLastAccess(any()) }
    }
}
