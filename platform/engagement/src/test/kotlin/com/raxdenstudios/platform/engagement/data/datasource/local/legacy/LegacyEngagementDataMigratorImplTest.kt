package com.raxdenstudios.platform.engagement.data.datasource.local.legacy

import android.content.SharedPreferences
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.database.dao.EngagementDao
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LegacyEngagementDataMigratorImplTest {

    private val editor: SharedPreferences.Editor = mockk()
    private val sharedPreferences: SharedPreferences = mockk()
    private val engagementDao: EngagementDao = mockk()
    private val database: EngagementDatabase = mockk()
    private val engagementEntityDataMapper: EngagementEntityDataMapper = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val stateEntity = EngagementStateEntity(
        hasRated = false,
        hasShared = false,
        shareDeclines = 0,
        lastSharePromptAt = null,
        reviewAttempts = 0,
        lastReviewAt = null,
        pendingPrompt = false,
    )
    private val originCountEntities = listOf(EngagementOriginCountEntity(origin = "app_opened", count = 1))

    private lateinit var migrator: LegacyEngagementDataMigratorImpl

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { any<RoomDatabase>().withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }
        every { database.engagementDao() } returns engagementDao
        coEvery { engagementDao.getState() } returns null
        coEvery { engagementDao.replace(any(), any()) } returns Unit
        every { engagementEntityDataMapper.toStateEntity(any()) } returns stateEntity
        every { engagementEntityDataMapper.toOriginCountEntities(any()) } returns originCountEntities
        every { sharedPreferences.edit() } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.commit() } returns true

        migrator = LegacyEngagementDataMigratorImpl(
            sharedPreferences = sharedPreferences,
            database = database,
            engagementEntityDataMapper = engagementEntityDataMapper,
            dispatcherProvider = dispatcherProvider,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `Given no legacy payload When migrated Then nothing is inserted nor removed`() = runTest {
        givenPayload(null)

        val result = migrator.migrate()

        assertEquals(Answer.Success(Unit), result)
        coVerify(exactly = 0) { engagementDao.replace(any(), any()) }
        verify(exactly = 0) { sharedPreferences.edit() }
    }

    @Test
    fun `Given a payload and no stored state When migrated Then data is inserted and key removed`() = runTest {
        givenPayload("""{"hasRated":false,"reviewAttempts":0,"counts":{"app_opened":1}}""")

        val result = migrator.migrate()

        assertEquals(Answer.Success(Unit), result)
        coVerify(exactly = 1) { engagementDao.replace(stateEntity, originCountEntities) }
        verify { editor.remove(ENGAGEMENT_DATA_KEY) }
        verify { editor.commit() }
    }

    @Test
    fun `Given a payload and stored state When migrated Then nothing is inserted and key removed`() = runTest {
        givenPayload("""{"hasRated":false,"reviewAttempts":0}""")
        coEvery { engagementDao.getState() } returns stateEntity

        val result = migrator.migrate()

        assertEquals(Answer.Success(Unit), result)
        coVerify(exactly = 0) { engagementDao.replace(any(), any()) }
        verify { editor.remove(ENGAGEMENT_DATA_KEY) }
    }

    @Test
    fun `Given a corrupt payload When migrated Then key is removed and nothing is inserted`() = runTest {
        givenPayload("{ not valid json")

        val result = migrator.migrate()

        assertEquals(Answer.Success(Unit), result)
        coVerify(exactly = 0) { engagementDao.replace(any(), any()) }
        verify { editor.remove(ENGAGEMENT_DATA_KEY) }
    }

    @Test
    fun `Given a legacy payload with hasRated and no reviewAttempts When migrated Then it counts as one attempt`() =
        runTest {
            givenPayload("""{"hasRated":true,"hasShared":true,"counts":{"app_opened":4},"lastSharePromptAt":7}""")

            migrator.migrate()

            val migrated = captureMigratedData()
            assertEquals(
                EngagementData(
                    hasRated = false,
                    hasShared = true,
                    counts = mapOf("app_opened" to 4),
                    lastSharePromptAt = 7L,
                    reviewAttempts = 1,
                    lastReviewAt = null,
                ),
                migrated,
            )
        }

    @Test
    fun `Given a legacy payload with hasRated and a lastReviewAt When migrated Then lastReviewAt is cleared`() =
        runTest {
            givenPayload("""{"hasRated":true,"lastReviewAt":500}""")

            migrator.migrate()

            assertEquals(EngagementData(hasRated = false, reviewAttempts = 1), captureMigratedData())
        }

    @Test
    fun `Given a legacy payload without hasRated When migrated Then it is not changed`() = runTest {
        givenPayload("""{"hasRated":false,"counts":{"app_opened":4}}""")

        migrator.migrate()

        assertEquals(EngagementData(counts = mapOf("app_opened" to 4)), captureMigratedData())
    }

    @Test
    fun `Given a current payload with reviewAttempts and hasRated When migrated Then it is unchanged`() = runTest {
        givenPayload("""{"hasRated":true,"reviewAttempts":0}""")

        migrator.migrate()

        assertEquals(EngagementData(hasRated = true, reviewAttempts = 0), captureMigratedData())
    }

    @Test
    fun `Given a payload with unknown keys When migrated Then unknown keys are ignored`() = runTest {
        givenPayload("""{"hasShared":true,"reviewAttempts":2,"some_future_field":42,"pendingPrompt":true}""")

        migrator.migrate()

        assertEquals(
            EngagementData(hasShared = true, reviewAttempts = 2, pendingPrompt = true),
            captureMigratedData(),
        )
    }

    @Test
    fun `Given the database throws When migrated Then Unknown failure is returned and key is kept`() = runTest {
        givenPayload("""{"hasRated":false,"reviewAttempts":0}""")
        coEvery { engagementDao.getState() } throws IllegalStateException("boom")

        val result = migrator.migrate()

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
        verify(exactly = 0) { editor.remove(any()) }
    }

    private fun givenPayload(
        payload: String?,
    ) {
        every { sharedPreferences.getString(ENGAGEMENT_DATA_KEY, null) } returns payload
    }

    private fun captureMigratedData(): EngagementData {
        val captured = mutableListOf<EngagementData>()
        verify { engagementEntityDataMapper.toStateEntity(capture(captured)) }
        return captured.single()
    }

    private companion object {
        const val ENGAGEMENT_DATA_KEY = "engagement_data"
    }
}
