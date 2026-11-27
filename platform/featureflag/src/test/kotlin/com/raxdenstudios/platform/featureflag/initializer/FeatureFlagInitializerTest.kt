package com.raxdenstudios.platform.featureflag.initializer

import android.content.Context
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.test.TestCoroutineScopeProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.core.initializer.DIInitializer
import com.raxdenstudios.platform.featureflag.domain.FetchFeatureFlagsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeatureFlagInitializerTest {

    private val fetchFeatureFlagsUseCase: FetchFeatureFlagsUseCase = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Given the fetch succeeds When it is created Then the flags are fetched once`() = runTest {
        coEvery { fetchFeatureFlagsUseCase(any()) } returns Answer.Success(Unit)

        createInitializer()

        coVerify(exactly = 1) { fetchFeatureFlagsUseCase(FetchFeatureFlagsUseCase.Params) }
    }

    @Test
    fun `Given the fetch fails When it is created Then the flags are fetched once and nothing throws`() = runTest {
        coEvery { fetchFeatureFlagsUseCase(any()) } returns Answer.Failure(Failure.Unknown("boom"))

        createInitializer()

        coVerify(exactly = 1) { fetchFeatureFlagsUseCase(FetchFeatureFlagsUseCase.Params) }
    }

    @Test
    fun `When dependencies are requested Then DIInitializer is the only one`() {
        assertEquals(listOf(DIInitializer::class.java), FeatureFlagInitializer().dependencies())
    }

    private fun TestScope.createInitializer() {
        val scopeProvider: CoroutineScopeProvider = TestCoroutineScopeProvider(backgroundScope)
        startKoin {
            modules(
                module {
                    single { scopeProvider }
                    single { fetchFeatureFlagsUseCase }
                },
            )
        }
        FeatureFlagInitializer().create(context)
        runCurrent()
    }
}
