package com.raxdenstudios.platform.featureflag.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.featureflag.data.repository.FeatureFlagRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FetchFeatureFlagsUseCaseImplTest {

    private val featureFlagRepository: FeatureFlagRepository = mockk()
    private val useCase = FetchFeatureFlagsUseCaseImpl(featureFlagRepository = featureFlagRepository)

    @Test
    fun `Given the repository succeeds When invoked Then returns success`() = runTest {
        coEvery { featureFlagRepository.fetch() } returns Answer.Success(Unit)

        val result = useCase(FetchFeatureFlagsUseCase.Params)

        assertEquals(Answer.Success(Unit), result)
        coVerify(exactly = 1) { featureFlagRepository.fetch() }
    }

    @Test
    fun `Given the repository fails When invoked Then returns the same failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { featureFlagRepository.fetch() } returns failure

        val result = useCase(FetchFeatureFlagsUseCase.Params)

        assertEquals(failure, result)
    }
}
