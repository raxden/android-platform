package com.raxdenstudios.platform.device.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.android.util.SDK
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.device.data.datasource.AppAccessLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

internal class AppRepositoryImplTest {

    private val context: Context = mockk()
    private val appAccessLocalDataSource: AppAccessLocalDataSource = mockk()
    private val repository = AppRepositoryImpl(context, appAccessLocalDataSource)

    @After
    fun tearDown() {
        unmockkObject(SDK)
    }

    @Test
    fun `getLastAccess delegates to local datasource`() = runTest {
        coEvery { appAccessLocalDataSource.getLastAccess() } returns Answer.Success(123L)

        val result = repository.getLastAccess()

        assertThat(result).isEqualTo(Answer.Success(123L))
        coVerify { appAccessLocalDataSource.getLastAccess() }
    }

    @Test
    fun `saveLastAccess delegates to local datasource`() = runTest {
        coEvery { appAccessLocalDataSource.saveLastAccess(123L) } returns Answer.Success(Unit)

        val result = repository.saveLastAccess(123L)

        assertThat(result).isEqualTo(Answer.Success(Unit))
        coVerify { appAccessLocalDataSource.saveLastAccess(123L) }
    }

    @Test
    fun `getAppVersion returns version name`() = runTest {
        mockkObject(SDK)
        every { SDK.getVersionName(context) } returns "1.2.3"

        val result = repository.getAppVersion()

        assertThat(result).isEqualTo(Answer.Success("1.2.3"))
    }
}
