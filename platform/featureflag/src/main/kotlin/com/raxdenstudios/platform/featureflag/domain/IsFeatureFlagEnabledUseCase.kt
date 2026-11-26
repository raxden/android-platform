package com.raxdenstudios.platform.featureflag.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.getValueOrDefault
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.featureflag.data.repository.FeatureFlagRepository
import com.raxdenstudios.platform.featureflag.domain.model.FeatureFlag

/**
 * Whether [Params.flag] is on. Waits briefly for the fetch started at app launch, then answers with
 * the last activated remote value; when the flag cannot be read it answers the flag's
 * [FeatureFlag.defaultValue], so it always succeeds.
 *
 * ```kotlin
 * val enabled = isFeatureFlagEnabledUseCase(
 *     IsFeatureFlagEnabledUseCase.Params(AppFeatureFlag.PODCASTS),
 * ).getValueOrDefault(AppFeatureFlag.PODCASTS.defaultValue)
 * ```
 */
interface IsFeatureFlagEnabledUseCase : SuspendUseCase<IsFeatureFlagEnabledUseCase.Params, Boolean> {

    data class Params(
        val flag: FeatureFlag,
    )
}

internal class IsFeatureFlagEnabledUseCaseImpl(
    private val featureFlagRepository: FeatureFlagRepository,
) : IsFeatureFlagEnabledUseCase {

    override suspend fun invoke(
        params: IsFeatureFlagEnabledUseCase.Params,
    ): Answer<Boolean, Failure> = Answer.Success(
        featureFlagRepository.isEnabled(params.flag).getValueOrDefault(params.flag.defaultValue),
    )
}
