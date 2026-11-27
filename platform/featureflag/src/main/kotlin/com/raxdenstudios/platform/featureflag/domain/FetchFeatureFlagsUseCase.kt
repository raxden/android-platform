package com.raxdenstudios.platform.featureflag.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.featureflag.data.repository.FeatureFlagRepository

/**
 * Fetches the remote values of every registered
 * [com.raxdenstudios.platform.featureflag.domain.model.FeatureFlag] and activates them. Meant to run
 * once per process, at app start; reads made meanwhile wait for it at most 2 s.
 *
 * Each step (setting the defaults, applying the settings and the fetch) has its own 10 s timeout, so a
 * full run can take up to about 30 s; this never delays reads beyond 2 s.
 *
 * The result is `Answer.Success` once the values are activated and `Answer.Failure` when setting the
 * defaults, the settings or the fetch fails (for example offline or after a step's timeout). A failure
 * is not fatal: reads keep answering the last activated values or the flag defaults, and a later
 * process start fetches again.
 *
 * ```kotlin
 * fetchFeatureFlagsUseCase(FetchFeatureFlagsUseCase.Params)
 * ```
 */
interface FetchFeatureFlagsUseCase : SuspendUseCase<FetchFeatureFlagsUseCase.Params, Unit> {

    data object Params
}

internal class FetchFeatureFlagsUseCaseImpl(
    private val featureFlagRepository: FeatureFlagRepository,
) : FetchFeatureFlagsUseCase {

    override suspend fun invoke(
        params: FetchFeatureFlagsUseCase.Params,
    ): Answer<Unit, Failure> = featureFlagRepository.fetch()
}
