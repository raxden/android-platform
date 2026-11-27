package com.raxdenstudios.platform.device.data.datasource

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.android.util.SDK
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

internal class AppVersionLocalDataSourceImplTest {

    private val context: Context = mockk()
    private val dataSource = AppVersionLocalDataSourceImpl(context)

    @After
    fun tearDown() {
        unmockkObject(SDK)
    }

    @Test
    fun `Given version code available When getInstalledVersionCode Then returns it`() = runTest {
        mockkObject(SDK)
        every { SDK.getVersionCode(context) } returns 42L

        val result = dataSource.getInstalledVersionCode()

        assertThat(result).isEqualTo(Answer.Success(42L))
    }

    @Test
    fun `Given version code is zero When getInstalledVersionCode Then returns unknown failure`() = runTest {
        mockkObject(SDK)
        every { SDK.getVersionCode(context) } returns 0L

        val result = dataSource.getInstalledVersionCode()

        assertThat(result).isEqualTo(
            Answer.Failure(Failure.Unknown("Installed version code could not be read")),
        )
    }
}
