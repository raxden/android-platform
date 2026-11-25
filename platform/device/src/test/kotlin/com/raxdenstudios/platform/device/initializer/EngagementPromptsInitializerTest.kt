package com.raxdenstudios.platform.device.initializer

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import com.raxdenstudios.platform.device.inapp.InAppReview
import com.raxdenstudios.platform.device.inapp.InAppShare
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

internal class EngagementPromptsInitializerTest {

    private val inAppReview: InAppReview = mockk(relaxed = true)
    private val inAppShare: InAppShare = mockk(relaxed = true)
    private val callbacks = slot<Application.ActivityLifecycleCallbacks>()
    private val application: Application = mockk(relaxed = true)

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Given no filter bound When a component activity is created Then both prompts are attached`() {
        startKoin { modules(engagementModule()) }
        createInitializer()
        val activity: ComponentActivity = mockk()

        callbacks.captured.onActivityCreated(activity, null)

        verify { inAppReview.attach(activity) }
        verify { inAppShare.attach(activity) }
    }

    @Test
    fun `Given no filter bound When a plain activity is created Then no prompt is attached`() {
        startKoin { modules(engagementModule()) }
        createInitializer()

        callbacks.captured.onActivityCreated(mockk<Activity>(), null)

        verify(exactly = 0) { inAppReview.attach(any()) }
        verify(exactly = 0) { inAppShare.attach(any()) }
    }

    @Test
    fun `Given a filter rejecting activities When a component activity is created Then no prompt is attached`() {
        startKoin { modules(engagementModule(EngagementPromptsActivityFilter { false })) }
        createInitializer()

        callbacks.captured.onActivityCreated(mockk<ComponentActivity>(), null)

        verify(exactly = 0) { inAppReview.attach(any()) }
        verify(exactly = 0) { inAppShare.attach(any()) }
    }

    @Test
    fun `Given a filter accepting activities When a component activity is created Then both prompts are attached`() {
        val received = mutableListOf<Activity>()
        val filter = EngagementPromptsActivityFilter { activity ->
            received.add(activity)
            true
        }
        startKoin { modules(engagementModule(filter)) }
        createInitializer()
        val activity: ComponentActivity = mockk()

        callbacks.captured.onActivityCreated(activity, null)

        assertTrue(received == listOf<Activity>(activity))
        verify { inAppReview.attach(activity) }
        verify { inAppShare.attach(activity) }
    }

    @Test
    fun `Given koin not started When create is called Then koin is not touched and attach works once started`() {
        createInitializer()
        startKoin { modules(engagementModule()) }
        val activity: ComponentActivity = mockk()

        callbacks.captured.onActivityCreated(activity, null)

        verify { inAppReview.attach(activity) }
        verify { inAppShare.attach(activity) }
    }

    @Test
    fun `When dependencies are requested Then they are empty`() {
        assertTrue(EngagementPromptsInitializer().dependencies().isEmpty())
    }

    private fun engagementModule(
        filter: EngagementPromptsActivityFilter? = null,
    ): Module = module {
        single { inAppReview }
        single { inAppShare }
        if (filter != null) single { filter }
    }

    private fun createInitializer() {
        every { application.applicationContext } returns application
        every { application.registerActivityLifecycleCallbacks(capture(callbacks)) } returns Unit
        EngagementPromptsInitializer().create(application as Context)
    }
}
