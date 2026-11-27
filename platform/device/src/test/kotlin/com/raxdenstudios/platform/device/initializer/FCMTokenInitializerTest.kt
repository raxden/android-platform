package com.raxdenstudios.platform.device.initializer

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.test.TestCoroutineScopeProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.core.initializer.DIInitializer
import com.raxdenstudios.platform.device.domain.usecase.GetFCMTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
internal class FCMTokenInitializerTest {

    private val getFCMTokenUseCase: GetFCMTokenUseCase = mockk()

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Given debuggable app and token available When create Then the token is requested once`() = runTest {
        coEvery { getFCMTokenUseCase(any()) } returns Answer.Success("token")

        createInitializer(ApplicationInfo.FLAG_DEBUGGABLE)

        coVerify(exactly = 1) { getFCMTokenUseCase(GetFCMTokenUseCase.Params) }
    }

    @Test
    fun `Given debuggable app and token fails When create Then the token is requested once and nothing throws`() =
        runTest {
            coEvery { getFCMTokenUseCase(any()) } returns Answer.Failure(Failure.Unknown("boom"))

            createInitializer(ApplicationInfo.FLAG_DEBUGGABLE or ApplicationInfo.FLAG_HAS_CODE)

            coVerify(exactly = 1) { getFCMTokenUseCase(GetFCMTokenUseCase.Params) }
        }

    @Test
    fun `Given non debuggable app When create Then the token is not requested`() = runTest {
        createInitializer(0)

        coVerify(exactly = 0) { getFCMTokenUseCase(any()) }
    }

    @Test
    fun `When dependencies are requested Then DIInitializer and TimberInitializer are listed`() {
        assertThat(FCMTokenInitializer().dependencies())
            .containsExactly(DIInitializer::class.java, TimberInitializer::class.java)
            .inOrder()
    }

    private fun TestScope.createInitializer(
        flags: Int,
    ) {
        val scopeProvider: CoroutineScopeProvider = TestCoroutineScopeProvider(backgroundScope)
        startKoin {
            modules(
                module {
                    single { scopeProvider }
                    single { getFCMTokenUseCase }
                },
            )
        }
        FCMTokenInitializer().create(contextWithFlags(flags))
        runCurrent()
    }

    private fun contextWithFlags(
        flags: Int,
    ): Context {
        val info = mockk<ApplicationInfo>().apply { this.flags = flags }
        return mockk<Context>().also { every { it.applicationInfo } returns info }
    }
}
