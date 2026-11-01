package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.AppRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetLastAccessUseCaseImplTest {

    private val appRepository: AppRepository = mockk()
    private val useCase = GetLastAccessUseCaseImpl(appRepository)

    @Test
    fun `invoke returns repository answer`() = runTest {
        coEvery { appRepository.getLastAccess() } returns Answer.Success(123L)

        val result = useCase(GetLastAccessUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(123L))
        coVerify { appRepository.getLastAccess() }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val failure = Failure.Unknown("error")
        coEvery { appRepository.getLastAccess() } returns Answer.Failure(failure)

        val result = useCase(GetLastAccessUseCase.Params)

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
