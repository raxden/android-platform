package com.raxdenstudios.platform.notification.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NotificationPermissionStateTest {

    @Test
    fun `Given each state When isGranted Then only Granted is true`() {
        val granted = NotificationPermissionState.entries.filter { it.isGranted }

        assertThat(granted).containsExactly(NotificationPermissionState.Granted)
    }

    @Test
    fun `Given each state When isRuntimePermissionMissing Then only Requestable and Blocked are true`() {
        val missing = NotificationPermissionState.entries.filter { it.isRuntimePermissionMissing }

        assertThat(missing).containsExactly(
            NotificationPermissionState.Requestable,
            NotificationPermissionState.Blocked,
        )
    }
}
