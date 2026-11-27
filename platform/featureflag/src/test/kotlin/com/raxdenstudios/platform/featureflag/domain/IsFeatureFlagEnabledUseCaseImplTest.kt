package com.raxdenstudios.platform.featureflag.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.featureflag.data.repository.FeatureFlagRepository
import com.raxdenstudios.platform.featureflag.domain.model.TestFeatureFlag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class IsFeatureFlagEnabledUseCaseImplTest {

    private val featureFlagRepository: FeatureFlagRepository = mockk()
    private val useCase = IsFeatureFlagEnabledUseCaseImpl(featureFlagRepository = featureFlagRepository)

    @Test
    fun `Given the repository answers true When invoked Then returns true`() = runTest {
        coEvery { featureFlagRepository.isEnabled(TestFeatureFlag.PODCASTS) } returns Answer.Success(true)

        val result = useCase(IsFeatureFlagEnabledUseCase.Params(TestFeatureFlag.PODCASTS))

        assertEquals(Answer.Success(true), result)
        coVerify(exactly = 1) { featureFlagRepository.isEnabled(TestFeatureFlag.PODCASTS) }
    }

    @Test
    fun `Given the repository answers false When invoked Then returns false`() = runTest {
        coEvery { featureFlagRepository.isEnabled(TestFeatureFlag.PODCASTS) } returns Answer.Success(false)

        val result = useCase(IsFeatureFlagEnabledUseCase.Params(TestFeatureFlag.PODCASTS))

        assertEquals(Answer.Success(false), result)
    }

    @Test
    fun `Given the repository fails When invoked Then returns the flag default value`() = runTest {
        coEvery { featureFlagRepository.isEnabled(any()) } returns Answer.Failure(mockk())

        val result = useCase(IsFeatureFlagEnabledUseCase.Params(TestFeatureFlag.DARK_MODE))

        assertEquals(Answer.Success(TestFeatureFlag.DARK_MODE.defaultValue), result)
    }
}
