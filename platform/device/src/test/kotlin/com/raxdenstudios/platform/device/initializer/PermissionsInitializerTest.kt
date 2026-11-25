package com.raxdenstudios.platform.device.initializer

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import com.raxdenstudios.commons.permissions.PermissionsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

internal class PermissionsInitializerTest {

    private val permissionsManager: PermissionsManager = mockk(relaxed = true)
    private val callbacks = slot<Application.ActivityLifecycleCallbacks>()
    private val application: Application = mockk(relaxed = true)

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Given a component activity When it is created Then it is attached to the permissions manager`() {
        startPermissionsKoin()
        createInitializer()
        val activity: ComponentActivity = mockk()

        callbacks.captured.onActivityCreated(activity, null)

        verify { permissionsManager.attach(activity) }
    }

    @Test
    fun `Given a plain activity When it is created Then it is not attached`() {
        startPermissionsKoin()
        createInitializer()

        callbacks.captured.onActivityCreated(mockk<Activity>(), null)

        verify(exactly = 0) { permissionsManager.attach(any()) }
    }

    @Test
    fun `Given koin not started When create is called Then koin is not touched and attach works once started`() {
        createInitializer()
        startPermissionsKoin()
        val activity: ComponentActivity = mockk()

        callbacks.captured.onActivityCreated(activity, null)

        verify { permissionsManager.attach(activity) }
    }

    @Test
    fun `When dependencies are requested Then they are empty`() {
        assertTrue(PermissionsInitializer().dependencies().isEmpty())
    }

    private fun startPermissionsKoin() {
        startKoin { modules(module { single { permissionsManager } }) }
    }

    private fun createInitializer() {
        every { application.applicationContext } returns application
        every { application.registerActivityLifecycleCallbacks(capture(callbacks)) } returns Unit
        PermissionsInitializer().create(application as Context)
    }
}
