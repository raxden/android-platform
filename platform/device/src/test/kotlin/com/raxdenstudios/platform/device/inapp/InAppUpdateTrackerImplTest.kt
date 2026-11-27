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

internal class InAppUpdateTrackerImplTest {

    private val params = mutableMapOf<String, Any>()
    private val firebaseAnalytics: FirebaseAnalytics = mockk {
        every { logEvent(any(), any()) } just Runs
    }
    private val tracker = InAppUpdateTrackerImpl(firebaseAnalytics)

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
    fun `Given an available update When checked Then logs checked with update details`() {
        tracker.checked(available = true, versionCode = 2, priority = 4, stalenessDays = 3)

        verify { firebaseAnalytics.logEvent("in_app_update_checked", any()) }
        assertThat(params).containsExactly(
            "available",
            "true",
            "version_code",
            2L,
            "priority",
            4L,
            "staleness_days",
            3L,
        )
    }

    @Test
    fun `Given no update and unknown staleness When checked Then logs checked without staleness`() {
        tracker.checked(available = false, versionCode = 2, priority = 0, stalenessDays = null)

        verify { firebaseAnalytics.logEvent("in_app_update_checked", any()) }
        assertThat(params).containsExactly(
            "available",
            "false",
            "version_code",
            2L,
            "priority",
            0L,
        )
    }

    @Test
    fun `Given Play accepted the request When launched Then logs launched with started result`() {
        tracker.launched(InAppUpdateType.IMMEDIATE, InAppUpdateLaunchResult.Started)

        verify { firebaseAnalytics.logEvent("in_app_update_launched", any()) }
        assertThat(params).containsExactly("type", "immediate", "result", "started")
    }

    @Test
    fun `Given no pending update When launched Then logs launched with not_available result`() {
        tracker.launched(InAppUpdateType.IMMEDIATE, InAppUpdateLaunchResult.NotAvailable)

        verify { firebaseAnalytics.logEvent("in_app_update_launched", any()) }
        assertThat(params).containsExactly("type", "immediate", "result", "not_available")
    }

    @Test
    fun `Given a flow already running When launched Then logs launched with already_in_progress result`() {
        tracker.launched(InAppUpdateType.IMMEDIATE, InAppUpdateLaunchResult.AlreadyInProgress)

        verify { firebaseAnalytics.logEvent("in_app_update_launched", any()) }
        assertThat(params).containsExactly("type", "immediate", "result", "already_in_progress")
    }

    @Test
    fun `Given Play refused the update When launched Then logs launched with not_allowed result`() {
        tracker.launched(InAppUpdateType.IMMEDIATE, InAppUpdateLaunchResult.NotAllowed)

        verify { firebaseAnalytics.logEvent("in_app_update_launched", any()) }
        assertThat(params).containsExactly("type", "immediate", "result", "not_allowed")
    }

    @Test
    fun `Given no activity attached When failed NO_ACTIVITY Then logs failed with no_activity reason`() {
        tracker.failed(InAppUpdateTracker.Reason.NO_ACTIVITY)

        verify { firebaseAnalytics.logEvent("in_app_update_failed", any()) }
        assertThat(params).containsExactly("reason", "no_activity")
    }

    @Test
    fun `Given the flow could not start When failed ERROR Then logs failed with error reason`() {
        tracker.failed(InAppUpdateTracker.Reason.ERROR)

        verify { firebaseAnalytics.logEvent("in_app_update_failed", any()) }
        assertThat(params).containsExactly("reason", "error")
    }

    @Test
    fun `Given the user accepted the update When accepted Then logs in_app_update_accepted`() {
        tracker.accepted()

        verify { firebaseAnalytics.logEvent("in_app_update_accepted", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given the user left the flow When declined Then logs in_app_update_declined`() {
        tracker.declined()

        verify { firebaseAnalytics.logEvent("in_app_update_declined", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given the flow ended with an error result When flowFailed Then logs in_app_update_flow_failed`() {
        tracker.flowFailed()

        verify { firebaseAnalytics.logEvent("in_app_update_flow_failed", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given an interrupted update was relaunched When resumed Then logs in_app_update_resumed`() {
        tracker.resumed()

        verify { firebaseAnalytics.logEvent("in_app_update_resumed", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given flexible type When launched Then logs launched with flexible type`() {
        tracker.launched(InAppUpdateType.FLEXIBLE, InAppUpdateLaunchResult.Started)

        verify { firebaseAnalytics.logEvent("in_app_update_launched", any()) }
        assertThat(params).containsExactly("type", "flexible", "result", "started")
    }

    @Test
    fun `Given a flexible update finished downloading When downloaded Then logs in_app_update_downloaded`() {
        tracker.downloaded()

        verify { firebaseAnalytics.logEvent("in_app_update_downloaded", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given install handed to Play When completed Then logs in_app_update_completed`() {
        tracker.completed()

        verify { firebaseAnalytics.logEvent("in_app_update_completed", any()) }
        assertThat(params).isEmpty()
    }

    @Test
    fun `Given store listing opened When storeOpened Then logs in_app_update_store_opened`() {
        tracker.storeOpened()

        verify { firebaseAnalytics.logEvent("in_app_update_store_opened", any()) }
        assertThat(params).isEmpty()
    }
}
