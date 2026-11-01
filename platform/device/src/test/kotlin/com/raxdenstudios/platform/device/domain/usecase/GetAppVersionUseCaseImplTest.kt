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

internal class GetAppVersionUseCaseImplTest {

    private val appRepository: AppRepository = mockk()
    private val useCase = GetAppVersionUseCaseImpl(appRepository)

    @Test
    fun `invoke returns repository answer`() = runTest {
        coEvery { appRepository.getAppVersion() } returns Answer.Success("1.0.0")

        val result = useCase(GetAppVersionUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success("1.0.0"))
        coVerify { appRepository.getAppVersion() }
    }

    @Test
    fun `invoke propagates failure`() = runTest {
        val failure = Failure.Unknown("error")
        coEvery { appRepository.getAppVersion() } returns Answer.Failure(failure)

        val result = useCase(GetAppVersionUseCase.Params)

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }
}
