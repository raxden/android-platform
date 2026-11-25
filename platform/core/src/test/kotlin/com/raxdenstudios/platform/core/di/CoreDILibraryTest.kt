package com.raxdenstudios.platform.core.di

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.coroutines.CoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.DefaultCoroutineScopeProvider
import com.raxdenstudios.commons.coroutines.DefaultDispatcherProvider
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication

internal class CoreDILibraryTest {

    private val library = CoreDILibrary()
    private lateinit var koin: Koin

    @Before
    fun setUp() {
        koin = koinApplication {
            androidContext(mockk<Context>(relaxed = true))
            modules(library.getModules().map { it.get() })
        }.koin
    }

    @After
    fun tearDown() {
        koin.close()
        stopKoin()
    }

    @Test
    fun `When getModules Then returns one module`() {
        assertThat(library.getModules()).hasSize(1)
    }

    @Test
    fun `Given core library When resolving DispatcherProvider Then returns DefaultDispatcherProvider`() {
        assertThat(koin.get<DispatcherProvider>()).isInstanceOf(DefaultDispatcherProvider::class.java)
    }

    @Test
    fun `Given core library When resolving DispatcherProvider twice Then returns the same instance`() {
        assertThat(koin.get<DispatcherProvider>()).isSameInstanceAs(koin.get<DispatcherProvider>())
    }

    @Test
    fun `Given core library When resolving CoroutineScopeProvider Then returns DefaultCoroutineScopeProvider`() {
        assertThat(koin.get<CoroutineScopeProvider>()).isInstanceOf(DefaultCoroutineScopeProvider::class.java)
    }

    @Test
    fun `Given core library When resolving CoroutineScopeProvider twice Then returns the same instance`() {
        assertThat(koin.get<CoroutineScopeProvider>()).isSameInstanceAs(koin.get<CoroutineScopeProvider>())
    }
}
