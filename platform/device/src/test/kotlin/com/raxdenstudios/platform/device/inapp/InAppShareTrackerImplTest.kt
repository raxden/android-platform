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

internal class InAppShareTrackerImplTest {

    private val params = mutableMapOf<String, Any>()
    private val firebaseAnalytics: FirebaseAnalytics = mockk {
        every { logEvent(any(), any()) } just Runs
    }
    private val tracker = InAppShareTrackerImpl(firebaseAnalytics)

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
    fun `Given a target with package When targetChosen Then logs target chosen with package name`() {
        tracker.targetChosen("com.whatsapp")

        verify { firebaseAnalytics.logEvent("in_app_share_target_chosen", any()) }
        assertThat(params).containsExactly("package_name", "com.whatsapp")
    }

    @Test
    fun `Given a target without package When targetChosen Then logs target chosen without params`() {
        tracker.targetChosen(null)

        verify { firebaseAnalytics.logEvent("in_app_share_target_chosen", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given the sheet was closed When dismissed Then logs in_app_share_dismissed`() {
        tracker.dismissed()

        verify { firebaseAnalytics.logEvent("in_app_share_dismissed", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given no activity attached When failed NO_ACTIVITY Then logs failed with no_activity reason`() {
        tracker.failed(InAppShareTracker.Reason.NO_ACTIVITY)

        verify { firebaseAnalytics.logEvent("in_app_share_failed", any()) }
        assertThat(params).containsExactly("reason", "no_activity")
    }

    @Test
    fun `Given a share in progress When failed ALREADY_IN_PROGRESS Then logs failed with reason`() {
        tracker.failed(InAppShareTracker.Reason.ALREADY_IN_PROGRESS)

        verify { firebaseAnalytics.logEvent("in_app_share_failed", any()) }
        assertThat(params).containsExactly("reason", "already_in_progress")
    }

    @Test
    fun `Given the sheet could not be shown When failed ERROR Then logs failed with error reason`() {
        tracker.failed(InAppShareTracker.Reason.ERROR)

        verify { firebaseAnalytics.logEvent("in_app_share_failed", any()) }
        assertThat(params).containsExactly("reason", "error")
    }
}
