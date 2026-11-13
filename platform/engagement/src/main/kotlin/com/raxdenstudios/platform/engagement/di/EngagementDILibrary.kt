package com.raxdenstudios.platform.engagement.di

import com.raxdenstudios.platform.core.di.DILibrary
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.engagement.di.module.EngagementDIModule

/**
 * DI entry point of the engagement module: in-app review and share prompts gated by
 * per-origin occurrence milestones.
 *
 * Progress is persisted in the module's own `SharedPreferences` file (`platform_engagement`,
 * key `engagement_data`) serialized with a private `Json` (`ignoreUnknownKeys`,
 * `encodeDefaults`), so consumers bind neither `SharedPreferences` nor `Json`.
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
 *     factory<EngagementEligibility> { MyEligibility() }
 *     factory<ShareContentProvider> { MyShareContentProvider() }
 * }
 * ```
 *
 * ## Usage
 *
 * Call `EngagementUseCase` from each trigger point, giving every origin its own name:
 *
 * ```kotlin
 * safeLaunch {
 *     engagementUseCase(EngagementUseCase.Params("origin"))
 *         .onSuccess { result ->
 *             if (result == EngagementResult.ReviewUnavailable) {
 *                 // Optional: the review flow could not launch — offer the Play Store page.
 *                 context.startActivity(intentFactory.build(IntentType.PlayStore))
 *             }
 *         }
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
