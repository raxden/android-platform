package com.raxdenstudios.platform.device.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.AppUpdateRepository
import com.raxdenstudios.platform.device.domain.model.AppUpdatePolicy
import com.raxdenstudios.platform.device.domain.model.AppUpdateRequirement
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetAppUpdateRequirementUseCaseImplTest {

    private val repository: AppUpdateRepository = mockk()
    private val useCase = GetAppUpdateRequirementUseCaseImpl(repository)

    @Test
    fun `Given installed below min When invoked Then returns Required`() = runTest {
        stub(installed = 99, policy = AppUpdatePolicy.mock)

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(AppUpdateRequirement.Required(99, 100)))
    }

    @Test
    fun `Given installed below both thresholds When invoked Then Required wins over Optional`() = runTest {
        stub(installed = 1, policy = AppUpdatePolicy(minVersionCode = 50, recommendedVersionCode = 60))

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(AppUpdateRequirement.Required(1, 50)))
    }

    @Test
    fun `Given installed equal to min and below recommended When invoked Then returns Optional`() = runTest {
        stub(installed = 100, policy = AppUpdatePolicy.mock)

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(AppUpdateRequirement.Optional(100, 110)))
    }

    @Test
    fun `Given installed equal to recommended When invoked Then returns None`() = runTest {
        stub(installed = 110, policy = AppUpdatePolicy.mock)

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(AppUpdateRequirement.None))
    }

    @Test
    fun `Given zero thresholds When invoked Then returns None`() = runTest {
        stub(installed = 1, policy = AppUpdatePolicy(minVersionCode = 0, recommendedVersionCode = 0))

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Success(AppUpdateRequirement.None))
    }

    @Test
    fun `Given installed version fails When invoked Then propagates failure without reading policy`() = runTest {
        val failure = Failure.Unknown("no version")
        coEvery { repository.getInstalledVersionCode() } returns Answer.Failure(failure)

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Failure(failure))
        coVerify(exactly = 0) { repository.getPolicy() }
    }

    @Test
    fun `Given policy fails When invoked Then propagates failure`() = runTest {
        val failure = Failure.Unknown("no policy")
        coEvery { repository.getInstalledVersionCode() } returns Answer.Success(1L)
        coEvery { repository.getPolicy() } returns Answer.Failure(failure)

        val result = useCase(GetAppUpdateRequirementUseCase.Params)

        assertThat(result).isEqualTo(Answer.Failure(failure))
    }

    private fun stub(
        installed: Long,
        policy: AppUpdatePolicy,
    ) {
        coEvery { repository.getInstalledVersionCode() } returns Answer.Success(installed)
        coEvery { repository.getPolicy() } returns Answer.Success(policy)
    }
}
