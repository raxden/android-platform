package com.raxdenstudios.platform.featureflag.data.datasource.remote

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.featureflag.domain.model.TestFeatureFlag
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeatureFlagRemoteDataSourceImplTest {

    private val remoteConfig: FirebaseRemoteConfig = mockk()
    private val defaultsTask: Task<Void> = mockk()
    private val settingsTask: Task<Void> = mockk()
    private val fetchTask: Task<Boolean> = mockk()
    private val value: FirebaseRemoteConfigValue = mockk()
    private val defaults = slot<Map<String, Any>>()
    private val settings = slot<FirebaseRemoteConfigSettings>()
    private val dataSource = FeatureFlagRemoteDataSourceImpl(
        remoteConfig = remoteConfig,
        flags = TestFeatureFlag.entries,
        minimumFetchInterval = 1.hours,
    )

    private fun <T> Task<T>.completeWith(
        result: T?,
        error: Exception? = null,
    ) {
        every { isSuccessful } returns (error == null)
        every { isCanceled } returns false
        every { this@completeWith.result } returns result
        every { exception } returns error
        every { addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(this@completeWith)
            this@completeWith
        }
    }

    private fun stubSuccessfulFetch() {
        defaultsTask.completeWith(null)
        settingsTask.completeWith(null)
        fetchTask.completeWith(true)
        every { remoteConfig.setDefaultsAsync(capture(defaults)) } returns defaultsTask
        every { remoteConfig.setConfigSettingsAsync(capture(settings)) } returns settingsTask
        every { remoteConfig.fetchAndActivate() } returns fetchTask
    }

    private fun stubValue(
        source: Int = FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
        asBoolean: Boolean = true,
    ) {
        every { value.source } returns source
        every { value.asBoolean() } returns asBoolean
        every { remoteConfig.getValue(TestFeatureFlag.PODCASTS.key) } returns value
    }

    @Test
    fun `Given every step succeeds When fetchAndActivate Then returns success`() = runTest {
        stubSuccessfulFetch()

        assertEquals(Answer.Success(Unit), dataSource.fetchAndActivate())
    }

    @Test
    fun `Given every step succeeds When fetchAndActivate Then defaults contain every registered flag`() = runTest {
        stubSuccessfulFetch()

        dataSource.fetchAndActivate()

        assertEquals(TestFeatureFlag.entries.associate { it.key to it.defaultValue }, defaults.captured)
    }

    @Test
    fun `Given every step succeeds When fetchAndActivate Then settings use the interval and 10 seconds timeout`() =
        runTest {
            stubSuccessfulFetch()

            dataSource.fetchAndActivate()

            assertEquals(1.hours.inWholeSeconds, settings.captured.minimumFetchIntervalInSeconds)
            assertEquals(10.seconds.inWholeSeconds, settings.captured.fetchTimeoutInSeconds)
        }

    @Test
    fun `Given the defaults fail When fetchAndActivate Then returns failure and does not fetch`() = runTest {
        stubSuccessfulFetch()
        defaultsTask.completeWith(null, IllegalStateException("boom"))

        val result = dataSource.fetchAndActivate()

        assertTrue(result is Answer.Failure)
        verify(exactly = 0) { remoteConfig.setConfigSettingsAsync(any()) }
        verify(exactly = 0) { remoteConfig.fetchAndActivate() }
    }

    @Test
    fun `Given the settings fail When fetchAndActivate Then returns failure and does not fetch`() = runTest {
        stubSuccessfulFetch()
        settingsTask.completeWith(null, IllegalStateException("boom"))

        val result = dataSource.fetchAndActivate()

        assertTrue(result is Answer.Failure)
        verify(exactly = 0) { remoteConfig.fetchAndActivate() }
    }

    @Test
    fun `Given the fetch fails When fetchAndActivate Then returns failure`() = runTest {
        stubSuccessfulFetch()
        fetchTask.completeWith(null, IllegalStateException("boom"))

        assertTrue(dataSource.fetchAndActivate() is Answer.Failure)
    }

    @Test
    fun `Given the defaults fail When a read is waiting Then the read is released at once`() = runTest {
        stubSuccessfulFetch()
        stubValue(asBoolean = true)
        defaultsTask.completeWith(null, IllegalStateException("boom"))
        val read = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        runCurrent()

        dataSource.fetchAndActivate()

        assertEquals(Answer.Success(true), read.await())
        assertEquals(0L, currentTime)
    }

    @Test
    fun `Given the settings fail When a read is waiting Then the read is released at once`() = runTest {
        stubSuccessfulFetch()
        stubValue(asBoolean = true)
        settingsTask.completeWith(null, IllegalStateException("boom"))
        val read = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        runCurrent()

        dataSource.fetchAndActivate()

        assertEquals(Answer.Success(true), read.await())
        assertEquals(0L, currentTime)
    }

    @Test
    fun `Given the fetch fails When a read is waiting Then the read is released at once`() = runTest {
        stubSuccessfulFetch()
        stubValue(asBoolean = true)
        fetchTask.completeWith(null, IllegalStateException("boom"))
        val read = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        runCurrent()

        dataSource.fetchAndActivate()

        assertEquals(Answer.Success(true), read.await())
        assertEquals(0L, currentTime)
    }

    @Test
    fun `Given a static value source When isEnabled Then returns the flag default`() = runTest {
        stubValue(source = FirebaseRemoteConfig.VALUE_SOURCE_STATIC, asBoolean = true)
        stubSuccessfulFetch()
        dataSource.fetchAndActivate()

        assertEquals(
            Answer.Success(TestFeatureFlag.PODCASTS.defaultValue),
            dataSource.isEnabled(TestFeatureFlag.PODCASTS),
        )
    }

    @Test
    fun `Given a remote value true When isEnabled Then returns true`() = runTest {
        stubValue(source = FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asBoolean = true)
        stubSuccessfulFetch()
        dataSource.fetchAndActivate()

        assertEquals(Answer.Success(true), dataSource.isEnabled(TestFeatureFlag.PODCASTS))
    }

    @Test
    fun `Given a remote value false When isEnabled Then returns false`() = runTest {
        stubValue(source = FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asBoolean = false)
        stubSuccessfulFetch()
        dataSource.fetchAndActivate()

        val result = dataSource.isEnabled(TestFeatureFlag.PODCASTS)

        assertTrue(result is Answer.Success)
        assertFalse((result as Answer.Success).value)
    }

    @Test
    fun `Given asBoolean throws When isEnabled Then returns failure`() = runTest {
        stubSuccessfulFetch()
        every { value.source } returns FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
        every { value.asBoolean() } throws IllegalArgumentException("not a boolean")
        every { remoteConfig.getValue(TestFeatureFlag.PODCASTS.key) } returns value
        dataSource.fetchAndActivate()

        assertTrue(dataSource.isEnabled(TestFeatureFlag.PODCASTS) is Answer.Failure)
    }

    @Test
    fun `Given no fetch happened When isEnabled Then returns after about 2 seconds`() = runTest {
        stubValue(asBoolean = true)

        val read = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        runCurrent()
        advanceTimeBy(1.999.seconds)
        assertFalse(read.isCompleted)

        advanceTimeBy(1)
        runCurrent()

        assertTrue(read.isCompleted)
        assertEquals(Answer.Success(true), read.await())
        assertEquals(2_000L, currentTime)
    }

    @Test
    fun `Given a first read timed out When read again Then returns the remote value without waiting`() = runTest {
        stubValue(asBoolean = true)
        val first = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(2_000L, currentTime)
        assertTrue(first.isCompleted)

        val second = dataSource.isEnabled(TestFeatureFlag.PODCASTS)

        assertEquals(Answer.Success(true), second)
        assertEquals(2_000L, currentTime)
    }

    @Test
    fun `Given a timed out read and a static source When read again Then returns the default at once`() = runTest {
        stubValue(source = FirebaseRemoteConfig.VALUE_SOURCE_STATIC, asBoolean = true)
        val first = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        advanceTimeBy(2.seconds)
        runCurrent()
        assertTrue(first.isCompleted)

        val second = dataSource.isEnabled(TestFeatureFlag.PODCASTS)

        assertEquals(Answer.Success(TestFeatureFlag.PODCASTS.defaultValue), second)
        assertEquals(2_000L, currentTime)
    }

    @Test
    fun `Given a read is waiting When fetchAndActivate succeeds Then the read is released at once`() = runTest {
        stubSuccessfulFetch()
        stubValue(asBoolean = true)
        val read = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
        runCurrent()

        dataSource.fetchAndActivate()

        assertEquals(Answer.Success(true), read.await())
        assertEquals(0L, currentTime)
    }

    @Test
    fun `Given two concurrent reads and no fetch When 2 seconds pass Then both complete and later reads do not wait`() =
        runTest {
            stubValue(asBoolean = true)
            val first = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
            val second = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
            runCurrent()
            advanceTimeBy(1.999.seconds)
            assertFalse(first.isCompleted)
            assertFalse(second.isCompleted)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(Answer.Success(true), first.await())
            assertEquals(Answer.Success(true), second.await())
            assertEquals(2_000L, currentTime)
            assertEquals(Answer.Success(true), dataSource.isEnabled(TestFeatureFlag.PODCASTS))
            assertEquals(2_000L, currentTime)
        }

    @Test
    fun `Given a waiting read When it is cancelled Then it propagates and a later read still waits 2 seconds`() =
        runTest {
            stubValue(asBoolean = true)
            val cancelled = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
            runCurrent()
            advanceTimeBy(500)

            cancelled.cancel()
            runCurrent()

            assertTrue(cancelled.isCancelled)
            assertTrue(cancelled.getCompletionExceptionOrNull() is CancellationException)
            val later = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
            runCurrent()
            advanceTimeBy(1.999.seconds)
            assertFalse(later.isCompleted)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(Answer.Success(true), later.await())
            assertEquals(2_500L, currentTime)
        }

    @Test
    fun `Given a read waiting When another starts at 1_5 s Then it completes at 3_5 s and later reads do not wait`() =
        runTest {
            stubValue(asBoolean = true)
            val first = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
            runCurrent()
            advanceTimeBy(1.5.seconds)
            val second = async { dataSource.isEnabled(TestFeatureFlag.PODCASTS) }
            runCurrent()

            advanceTimeBy(1.999.seconds)
            assertFalse(second.isCompleted)
            advanceTimeBy(1)
            runCurrent()

            assertTrue(first.isCompleted)
            assertEquals(Answer.Success(true), second.await())
            assertEquals(3_500L, currentTime)
            assertEquals(Answer.Success(true), dataSource.isEnabled(TestFeatureFlag.PODCASTS))
            assertEquals(3_500L, currentTime)
        }
}
