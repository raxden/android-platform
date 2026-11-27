package com.raxdenstudios.platform.notification.data.datasource

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.notification.NotificationStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class NotificationPermissionLocalDataSourceImplTest {

    private val preferences: SharedPreferences = RuntimeEnvironment.getApplication()
        .getSharedPreferences(NotificationStorage.PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dataSource = NotificationPermissionLocalDataSourceImpl(preferences)

    @Test
    fun `Given nothing stored When wasRequested Then false`() = runTest {
        assertThat(dataSource.wasRequested()).isEqualTo(Answer.Success(false))
    }

    @Test
    fun `Given markAsRequested When wasRequested Then true under the permission key`() = runTest {
        assertThat(dataSource.markAsRequested()).isEqualTo(Answer.Success(Unit))

        assertThat(dataSource.wasRequested()).isEqualTo(Answer.Success(true))
        assertThat(preferences.getBoolean("permission_requested", false)).isTrue()
    }

    @Test
    fun `Given preferences throw When reading or writing Then returns failure`() = runTest {
        val broken: SharedPreferences = mockk()
        every { broken.getBoolean(any(), any()) } throws IllegalStateException("boom")
        every { broken.edit() } throws IllegalStateException("boom")
        val brokenDataSource = NotificationPermissionLocalDataSourceImpl(broken)

        assertThat(brokenDataSource.wasRequested()).isInstanceOf(Answer.Failure::class.java)
        assertThat(brokenDataSource.markAsRequested()).isInstanceOf(Answer.Failure::class.java)
    }
}
