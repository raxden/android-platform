package com.raxdenstudios.platform.device.domain.usecase

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.then
import com.raxdenstudios.commons.coroutines.ext.coFlatMap
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.data.repository.AppUpdateRepository
import com.raxdenstudios.platform.device.domain.model.AppUpdatePolicy
import com.raxdenstudios.platform.device.domain.model.AppUpdateRequirement

/**
 * Compares the installed `versionCode` with the remote [AppUpdatePolicy] and returns the [AppUpdateRequirement].
 *
 * Reads the installed version and the policy on every call; a required update wins over an optional one, and a
 * threshold of 0 is ignored. Fails only when the installed `versionCode` or the policy cannot be read.
 *
 * ## Setup
 *
 * Publish two integer keys in Firebase Remote Config. Both are `versionCode` values, compared directly with the
 * installed one:
 *
 * - `app_update_min_version_code`: installs below it must update before using the app
 *   ([AppUpdateRequirement.Required]).
 * - `app_update_recommended_version_code`: installs below it are offered an optional update
 *   ([AppUpdateRequirement.Optional]).
 *
 * A missing key reads as 0, which disables that threshold. Remote Config is fetched at most once per hour in release
 * builds and on every call in debuggable builds. A failed fetch keeps the last activated values, so an outage never
 * blocks the app.
 *
 * ## Usage
 *
 * ```kotlin
 * getAppUpdateRequirementUseCase(GetAppUpdateRequirementUseCase.Params).onSuccess { requirement ->
 *     when (requirement) {
 *         // If Play cannot run the flow or the user declines, show a blocking screen (InAppUpdate.openStoreListing).
 *         is AppUpdateRequirement.Required -> inAppUpdate.launch(InAppUpdateType.IMMEDIATE)
 *         is AppUpdateRequirement.Optional -> inAppUpdate.launch(InAppUpdateType.FLEXIBLE)
 *         AppUpdateRequirement.None -> Unit
 *     }
 * }
 * ```
 */
interface GetAppUpdateRequirementUseCase :
    SuspendUseCase<GetAppUpdateRequirementUseCase.Params, AppUpdateRequirement> {

    /** Input marker: the call takes no parameters, the installed version and policy are always read fresh. */
    data object Params
}

internal class GetAppUpdateRequirementUseCaseImpl(
    private val repository: AppUpdateRepository,
) : GetAppUpdateRequirementUseCase {

    override suspend fun invoke(
        params: GetAppUpdateRequirementUseCase.Params,
    ): Answer<AppUpdateRequirement, Failure> = repository.getInstalledVersionCode()
        .coFlatMap { installed -> repository.getPolicy().then { policy -> policy.requirementFor(installed) } }

    private fun AppUpdatePolicy.requirementFor(
        installedVersionCode: Long,
    ): AppUpdateRequirement = when {
        installedVersionCode < minVersionCode -> AppUpdateRequirement.Required(
            installedVersionCode = installedVersionCode,
            minVersionCode = minVersionCode,
        )

        installedVersionCode < recommendedVersionCode -> AppUpdateRequirement.Optional(
            installedVersionCode = installedVersionCode,
            recommendedVersionCode = recommendedVersionCode,
        )

        else -> AppUpdateRequirement.None
    }
}
