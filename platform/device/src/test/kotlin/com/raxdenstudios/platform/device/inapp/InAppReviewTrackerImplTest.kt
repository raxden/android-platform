package com.raxdenstudios.platform.device.inapp

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class InAppReviewTrackerImplTest {

    private val params = mutableMapOf<String, Any>()
    private val firebaseAnalytics: FirebaseAnalytics = mockk {
        every { logEvent(any(), any()) } just Runs
    }
    private val tracker = InAppReviewTrackerImpl(firebaseAnalytics)

    /**
     * [Bundle] is an android.jar stub in local unit tests, so the constructor mock records the
     * params written by `ParametersBuilder` and lets them be asserted per event.
     */
    @Before
    fun setUp() {
        mockkConstructor(Bundle::class)
        every { anyConstructed<Bundle>().putString(any(), any()) } answers {
            params[firstArg<String>()] = secondArg<String>()
        }
        every { anyConstructed<Bundle>().putLong(any(), any()) } answers {
            params[firstArg<String>()] = secondArg<Long>()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given the review flow was launched When launched Then logs in_app_review_launched`() {
        tracker.launched()

        verify { firebaseAnalytics.logEvent("in_app_review_launched", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given no activity attached When failed NO_ACTIVITY Then logs failed with no_activity reason`() {
        tracker.failed(InAppReviewTracker.Reason.NO_ACTIVITY)

        verify { firebaseAnalytics.logEvent("in_app_review_failed", any()) }
        assertThat(params).containsExactly("reason", "no_activity")
    }

    @Test
    fun `Given the flow could not be launched When failed ERROR Then logs failed with error reason`() {
        tracker.failed(InAppReviewTracker.Reason.ERROR)

        verify { firebaseAnalytics.logEvent("in_app_review_failed", any()) }
        assertThat(params).containsExactly("reason", "error")
    }
}
