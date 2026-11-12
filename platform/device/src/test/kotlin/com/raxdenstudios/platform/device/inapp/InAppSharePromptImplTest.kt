package com.raxdenstudios.platform.device.inapp

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.platform.ui.theme.ThemeProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [InAppSharePromptImpl].
 *
 * `show` renders `InAppSharePromptSheet` on a [ComposeView] attached to `android.R.id.content`.
 * The sheet internals (answer buttons, dismiss callback, hide animation) only run inside a real
 * composition, so they can only be exercised under instrumentation or Robolectric. Locally the
 * `ComposeView` is constructor-mocked and `setContent` never executes, which means the deferred
 * answer can only be completed through the [DefaultLifecycleObserver] the prompt registers on the
 * host activity (returning `false`) or by cancelling the awaiting coroutine. The tests below cover
 * the contract around that: observer registration, view attachment, disposal ordering, detach on
 * resolution and on cancellation, and that `show` can be invoked again once resolved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppSharePromptImplTest {

    private val themeProvider: ThemeProvider = mockk()
    private val observers = mutableListOf<LifecycleObserver>()
    private val lifecycle: Lifecycle = mockk {
        every { addObserver(capture(observers)) } just Runs
        every { removeObserver(any()) } just Runs
    }
    private val content: ViewGroup = mockk {
        every { addView(any<View>(), any<Int>(), any<Int>()) } just Runs
        every { removeView(any()) } just Runs
    }
    private val activity: ComponentActivity = mockk()
    private val prompt = InAppSharePromptImpl(themeProvider)

    /**
     * `new ComposeView(activity)` always runs the real constructor chain (mockk's constructor
     * mocking registers the instance only when each `<init>` exits, it cannot skip the bodies), and
     * that chain invokes `android.view` methods that are stubs in the local android.jar. Mocking
     * the constructors of the whole hierarchy registers the instance early enough (at
     * `View`/`ViewGroup` constructor exit) for every framework call inside
     * `AbstractComposeView.<init>` and `ComposeView.<init>` to be intercepted and stubbed here.
     */
    @Before
    fun setUp() {
        every { activity.lifecycle } returns lifecycle
        every { activity.findViewById<ViewGroup>(android.R.id.content) } returns content

        mockkConstructor(View::class)
        mockkConstructor(ViewGroup::class)
        mockkConstructor(AbstractComposeView::class)
        mockkConstructor(ComposeView::class)
        // Calls executed by ViewCompositionStrategy.Default.installFor(this) and the
        // AbstractComposeView init block while the ComposeView is being constructed.
        every { anyConstructed<ComposeView>().setClipChildren(any()) } just Runs
        every { anyConstructed<ComposeView>().setClipToPadding(any()) } just Runs
        every { anyConstructed<ComposeView>().setImportantForAccessibility(any()) } just Runs
        every { anyConstructed<ComposeView>().addOnAttachStateChangeListener(any()) } just Runs
        every { anyConstructed<ComposeView>().getTag(any()) } returns null
        every { anyConstructed<ComposeView>().setTag(any(), any()) } just Runs
        // Public API exercised by InAppSharePromptImpl.
        every { anyConstructed<ComposeView>().setViewCompositionStrategy(any()) } just Runs
        every { anyConstructed<ComposeView>().setContent(any()) } just Runs
        every { anyConstructed<ComposeView>().disposeComposition() } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given show is called When it awaits an answer Then the observer is registered and the view is attached`() =
        runTest {
            val job = launch { prompt.show(activity) }
            runCurrent()

            verify { lifecycle.addObserver(any()) }
            verify { anyConstructed<ComposeView>().setContent(any()) }
            verify {
                content.addView(
                    any(),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            job.cancel()
        }

    @Test
    fun `Given an open modal When the host activity is destroyed Then show returns false and detaches`() = runTest {
        var result: Boolean? = null
        launch { result = prompt.show(activity) }
        runCurrent()

        capturedObserver().onDestroy(activity)
        advanceUntilIdle()

        assertThat(result).isFalse()
        verify { lifecycle.removeObserver(observers.last()) }
        verify { anyConstructed<ComposeView>().disposeComposition() }
        verify { content.removeView(any()) }
    }

    @Test
    fun `Given the modal resolved When it is detached Then the composition is disposed before the view is removed`() =
        runTest {
            launch { prompt.show(activity) }
            runCurrent()

            capturedObserver().onDestroy(activity)
            advanceUntilIdle()

            verifyOrder {
                anyConstructed<ComposeView>().disposeComposition()
                content.removeView(any())
            }
        }

    @Test
    fun `Given a modal already answered When show is called again Then a new view is attached and resolved`() =
        runTest {
            launch { prompt.show(activity) }
            runCurrent()
            capturedObserver(index = 0).onDestroy(activity)
            advanceUntilIdle()

            var result: Boolean? = null
            launch { result = prompt.show(activity) }
            runCurrent()
            capturedObserver(index = 1).onDestroy(activity)
            advanceUntilIdle()

            assertThat(result).isFalse()
            assertThat(observers).hasSize(2)
            verify(exactly = 2) { content.addView(any<View>(), any<Int>(), any<Int>()) }
            verify(exactly = 2) { content.removeView(any()) }
            verify { lifecycle.removeObserver(observers[0]) }
            verify { lifecycle.removeObserver(observers[1]) }
        }

    @Test
    fun `Given an open modal When the awaiting coroutine is cancelled Then the observer and view are removed`() =
        runTest {
            val job = launch { prompt.show(activity) }
            runCurrent()

            job.cancel()
            advanceUntilIdle()

            assertThat(job.isCompleted).isTrue()
            verify { lifecycle.removeObserver(observers.last()) }
            verifyOrder {
                anyConstructed<ComposeView>().disposeComposition()
                content.removeView(any())
            }
        }

    private fun capturedObserver(index: Int = observers.lastIndex): DefaultLifecycleObserver =
        observers[index] as DefaultLifecycleObserver
}
