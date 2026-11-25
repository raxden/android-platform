package com.raxdenstudios.platform.device.initializer

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.device.logging.FirebaseCrashlyticsTree
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

internal class TimberInitializerTest {

    private val initializer = TimberInitializer()

    @Before
    fun setUp() {
        Timber.uprootAll()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `Given debuggable app When create Then plants a single DebugTree`() {
        initializer.create(contextWithFlags(ApplicationInfo.FLAG_DEBUGGABLE))

        assertThat(Timber.forest().map { it::class.java }).containsExactly(Timber.DebugTree::class.java)
    }

    @Test
    fun `Given debuggable app with other flags When create Then plants a single DebugTree`() {
        initializer.create(contextWithFlags(ApplicationInfo.FLAG_DEBUGGABLE or ApplicationInfo.FLAG_HAS_CODE))

        assertThat(Timber.forest().map { it::class.java }).containsExactly(Timber.DebugTree::class.java)
    }

    @Test
    fun `Given non debuggable app When create Then plants a single FirebaseCrashlyticsTree`() {
        initializer.create(contextWithFlags(0))

        assertThat(Timber.forest().map { it::class.java }).containsExactly(FirebaseCrashlyticsTree::class.java)
    }

    @Test
    fun `Given previously planted tree When create Then keeps it and adds the new one`() {
        val previous = object : Timber.Tree() {
            override fun log(
                priority: Int,
                tag: String?,
                message: String,
                t: Throwable?,
            ) = Unit
        }
        Timber.plant(previous)

        initializer.create(contextWithFlags(0))

        assertThat(Timber.treeCount).isEqualTo(2)
        assertThat(Timber.forest()).contains(previous)
    }

    @Test
    fun `When dependencies Then is empty`() {
        assertThat(initializer.dependencies()).isEmpty()
    }

    private fun contextWithFlags(
        flags: Int,
    ): Context {
        val info = mockk<ApplicationInfo>().apply { this.flags = flags }
        return mockk<Context>().also { every { it.applicationInfo } returns info }
    }
}
