package com.raxdenstudios.platform.engagement.data.datasource.local

import android.content.SharedPreferences
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EngagementDataLocalDataSourceImplTest {

    /**
     * In-memory SharedPreferences: reads resolve against [prefsStorage] and writes through the
     * mocked editor land back in it, so persistence is exercised for real.
     */
    private val prefsStorage = mutableMapOf<String, String>()
    private val editor: SharedPreferences.Editor = mockk()
    private val sharedPreferences: SharedPreferences = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private lateinit var dataSource: EngagementDataLocalDataSourceImpl

    @Before
    fun setUp() {
        prefsStorage.clear()
        every { sharedPreferences.getString(any(), any()) } answers {
            prefsStorage[firstArg()] ?: secondArg()
        }
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            prefsStorage[firstArg()] = requireNotNull(secondArg<String>())
            editor
        }
        every { editor.apply() } just Runs

        dataSource = newDataSource()
    }

    @Test
    fun `Given no persisted data When data is read Then default data is returned`() = runTest {
        val result = dataSource.getEngagementData()

        assertEquals(Answer.Success(EngagementData()), result)
    }

    @Test
    fun `Given persisted data When data is read Then decoded data is returned`() = runTest {
        prefsStorage[ENGAGEMENT_DATA_KEY] =
            """{"hasRated":true,"hasShared":false,"counts":{"app_opened":3}}"""

        val result = dataSource.getEngagementData()

        assertEquals(
            Answer.Success(EngagementData(hasRated = true, counts = mapOf("app_opened" to 3))),
            result,
        )
    }

    @Test
    fun `Given an update When data is read back by a new instance Then it round-trips through preferences`() = runTest {
        val updated = dataSource.updateEngagementData(
            EngagementData(hasRated = true, counts = mapOf("app_opened" to 1)),
        )

        assertEquals(Answer.Success(Unit), updated)
        assertEquals(
            Answer.Success(EngagementData(hasRated = true, counts = mapOf("app_opened" to 1))),
            newDataSource().getEngagementData(),
        )
    }

    @Test
    fun `Given corrupt JSON When data is read Then it falls back to default data`() = runTest {
        prefsStorage[ENGAGEMENT_DATA_KEY] = "{ not valid json"

        val result = dataSource.getEngagementData()

        assertEquals(Answer.Success(EngagementData()), result)
    }

    @Test
    fun `Given JSON with unknown keys When data is read Then unknown keys are ignored`() = runTest {
        prefsStorage[ENGAGEMENT_DATA_KEY] =
            """{"hasRated":true,"some_future_field":42,"counts":{"app_opened":2}}"""

        val result = dataSource.getEngagementData()

        assertEquals(
            Answer.Success(EngagementData(hasRated = true, counts = mapOf("app_opened" to 2))),
            result,
        )
    }

    @Test
    fun `Given an observer When data is updated Then updates are emitted`() = runTest {
        val emissions = mutableListOf<Answer<EngagementData, Failure>>()
        val job = launch { dataSource.observeEngagementData().collect { emissions += it } }
        runCurrent()

        dataSource.updateEngagementData(EngagementData(hasRated = true))
        runCurrent()

        assertEquals(
            listOf(
                Answer.Success(EngagementData()),
                Answer.Success(EngagementData(hasRated = true)),
            ),
            emissions,
        )
        job.cancel()
    }

    private fun newDataSource() = EngagementDataLocalDataSourceImpl(
        sharedPreferences = sharedPreferences,
        dispatcherProvider = dispatcherProvider,
    )

    private companion object {
        const val ENGAGEMENT_DATA_KEY = "engagement_data"
    }
}
