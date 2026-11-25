package com.raxdenstudios.platform.engagement.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.di.module.EngagementDIModule

/**
 * DI entry point of the engagement module: in-app review and share prompts gated by
 * per-origin occurrence milestones, rate-limited by `EngagementPolicy`. With its defaults nothing
 * is shown during the first 3 days after install; the review is requested at most 3 times, 90 days
 * apart, and never again once the user rated the app manually; the share prompt is shown until
 * the user shares or declines it twice (closing the share sheet without a target counts as a
 * decline), 30 days apart; review and share prompts are at least 14 days apart. Milestones that
 * could not show anything (e.g. reached from a notification action with no activity) are kept
 * pending for the next call within 48 hours. See `EngagementUseCase` for the full policy.
 *
 * Progress is persisted in the module's own Room database (`platform_engagement.db`), so
 * consumers bind nothing for it. Progress stored by older versions in the `platform_engagement`
 * `SharedPreferences` file is not carried over: every install starts from zero.
 *
 * ## Setup
 *
 * Load this library together with `DeviceDILibrary`, which provides the `InAppReview` and
 * `InAppShare` helpers it depends on:
 *
 * ```kotlin
 * override fun provideLibs(): List<DILibrary> = listOf(
 *     DeviceDILibrary(),      // InAppReview + InAppShare
 *     EngagementDILibrary(),
 *     // ...
 * )
 * ```
 *
 * Attach both helpers to every activity, e.g. in `Application.ActivityLifecycleCallbacks`:
 *
 * ```kotlin
 * override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
 *     if (activity !is ComponentActivity) return
 *     inAppReview.attach(activity)
 *     inAppShare.attach(activity)   // must run before the activity is STARTED
 * }
 * ```
 *
 * Optionally bind a custom `EngagementEligibility` and/or `ShareContentProvider` in the app
 * modules; when absent, generic defaults apply (milestones at the 3rd occurrence, then every
 * 10th; share message built from the app label and its Play Store URL via the
 * `engagement_share_subject`/`engagement_share_text` strings, overridable by resource name):
 *
 * ```kotlin
 * module {
 *     factory<EngagementEligibility> {
 *         MilestoneEngagementEligibility(
 *             milestones = mapOf(
 *                 "favourite_added" to Milestones(early = setOf(3), step = 10),
 *             ),
 *         )
 *     }
 *     factory<ShareContentProvider> {
 *         StringResShareContentProvider(
 *             context = androidContext(),
 *             subject = R.string.share_subject,
 *             text = R.string.share_text, // formatted with the release package name
 *         )
 *     }
 * }
 * ```
 *
 * Origins missing from the `MilestoneEngagementEligibility` table are never eligible. Any other
 * `EngagementEligibility` or `ShareContentProvider` implementation can be bound the same way.
 *
 * To change the rate limits, override the `EngagementPolicy` bound by this library with a
 * `factory` in an app module loaded after it (listed after `EngagementDILibrary` in
 * `provideLibs()`), e.g. from Remote Config. Unset properties keep their defaults. It is resolved
 * once, when `EngagementUseCase` (a singleton) is first created:
 *
 * ```kotlin
 * module {
 *     factory {
 *         EngagementPolicy(
 *             shareCooldown = remoteConfig.getLong("engagement_share_cooldown_days").days,
 *             minInstallAge = remoteConfig.getLong("engagement_min_install_age_days").days,
 *         )
 *     }
 * }
 * ```
 *
 * ## Usage
 *
 * Call `EngagementUseCase` from each trigger point, giving every origin its own name:
 *
 * ```kotlin
 * // import com.raxdenstudios.commons.android.intent.IntentType
 * safeLaunch {
 *     engagementUseCase(EngagementUseCase.Params("origin"))
 *         .onSuccess { result ->
 *             if (result == EngagementResult.ReviewUnavailable) {
 *                 // Optional: the review flow could not launch — offer the Play Store page.
 *                 context.startActivity(intentFactory.build(IntentType.PlayStore))
 *                 markAppAsRatedUseCase(MarkAppAsRatedUseCase.Params)
 *             }
 *         }
 * }
 * ```
 *
 * Whenever the user opens the Play Store rating page manually (e.g. a "Rate us" settings entry),
 * call `MarkAppAsRatedUseCase` so the in-app review is never requested again:
 *
 * ```kotlin
 * // import com.raxdenstudios.commons.android.intent.IntentType
 * safeLaunch {
 *     context.startActivity(intentFactory.build(IntentType.PlayStore))
 *     markAppAsRatedUseCase(MarkAppAsRatedUseCase.Params)
 * }
 * ```
 */
class EngagementDILibrary : DILibrary {

    override fun initialize() {
        // empty block
    }

    override fun getModules(): List<DIModule> = listOf(
        EngagementDIModule(),
    )
}
