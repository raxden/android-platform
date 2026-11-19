package com.raxdenstudios.platform.engagement.data.datasource.local

import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.coroutines.DispatcherProvider
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.engagement.data.database.EngagementDatabase
import com.raxdenstudios.platform.engagement.data.database.dao.EngagementDao
import com.raxdenstudios.platform.engagement.data.database.model.EngagementOriginCountEntity
import com.raxdenstudios.platform.engagement.data.database.model.EngagementStateEntity
import com.raxdenstudios.platform.engagement.data.datasource.local.legacy.LegacyEngagementDataMigrator
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EngagementDataLocalDataSourceImplTest {

    private val engagementDao: EngagementDao = mockk()
    private val invalidationTracker: InvalidationTracker = mockk()
    private val database: EngagementDatabase = mockk()
    private val legacyEngagementDataMigrator: LegacyEngagementDataMigrator = mockk()
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
    private val originCountEntities = listOf(EngagementOriginCountEntity(origin = "app_opened", count = 3))
    private val storedData = EngagementData(counts = mapOf("app_opened" to 3))
    private val failure = Failure.Unknown("migration error")

    private lateinit var dataSource: EngagementDataLocalDataSourceImpl

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { any<RoomDatabase>().withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }
        every { database.engagementDao() } returns engagementDao
        every { database.invalidationTracker } returns invalidationTracker
        every { invalidationTracker.createFlow(*anyVararg(), emitInitialState = any()) } returns flowOf(emptySet())
        coEvery { legacyEngagementDataMigrator.migrate() } returns Answer.Success(Unit)
        coEvery { engagementDao.getState() } returns stateEntity
        coEvery { engagementDao.getOriginCounts() } returns originCountEntities
        coEvery { engagementDao.replace(any(), any()) } returns Unit
        every { engagementEntityDataMapper.toEngagementData(any(), any()) } returns storedData
        every { engagementEntityDataMapper.toStateEntity(any()) } returns stateEntity
        every { engagementEntityDataMapper.toOriginCountEntities(any()) } returns originCountEntities

        dataSource = EngagementDataLocalDataSourceImpl(
            database = database,
            legacyEngagementDataMigrator = legacyEngagementDataMigrator,
            engagementEntityDataMapper = engagementEntityDataMapper,
            dispatcherProvider = dispatcherProvider,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `Given stored data When data is read Then the mapped data is returned after migrating`() = runTest {
        val result = dataSource.getEngagementData()

        assertEquals(Answer.Success(storedData), result)
        coVerify { legacyEngagementDataMigrator.migrate() }
        coVerify { engagementEntityDataMapper.toEngagementData(stateEntity, originCountEntities) }
    }

    @Test
    fun `Given no stored state When data is read Then the mapper receives a null state`() = runTest {
        coEvery { engagementDao.getState() } returns null
        every { engagementEntityDataMapper.toEngagementData(null, any()) } returns EngagementData()

        val result = dataSource.getEngagementData()

        assertEquals(Answer.Success(EngagementData()), result)
    }

    @Test
    fun `Given migration fails When data is read Then failure is returned and the database is untouched`() = runTest {
        coEvery { legacyEngagementDataMigrator.migrate() } returns Answer.Failure(failure)

        val result = dataSource.getEngagementData()

        assertEquals(Answer.Failure(failure), result)
        coVerify(exactly = 0) { engagementDao.getState() }
    }

    @Test
    fun `Given the dao throws When data is read Then Unknown failure is returned`() = runTest {
        coEvery { engagementDao.getState() } throws IllegalStateException("boom")

        val result = dataSource.getEngagementData()

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
    }

    @Test
    fun `Given an update When applied Then it receives the stored data and the result is persisted`() = runTest {
        val updated = storedData.copy(hasRated = true)
        var received: EngagementData? = null

        val result = dataSource.updateEngagementData { current ->
            received = current
            updated
        }

        assertEquals(Answer.Success(updated), result)
        assertEquals(storedData, received)
        coVerify { engagementEntityDataMapper.toStateEntity(updated) }
        coVerify { engagementEntityDataMapper.toOriginCountEntities(updated) }
        coVerify(exactly = 1) { engagementDao.replace(stateEntity, originCountEntities) }
    }

    @Test
    fun `Given migration fails When data is updated Then failure is returned and nothing is written`() = runTest {
        coEvery { legacyEngagementDataMigrator.migrate() } returns Answer.Failure(failure)

        val result = dataSource.updateEngagementData { it.copy(hasRated = true) }

        assertEquals(Answer.Failure(failure), result)
        coVerify(exactly = 0) { engagementDao.replace(any(), any()) }
    }

    @Test
    fun `Given the dao throws on write When data is updated Then Unknown failure is returned`() = runTest {
        coEvery { engagementDao.replace(any(), any()) } throws IllegalStateException("boom")

        val result = dataSource.updateEngagementData { it.copy(hasRated = true) }

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
    }

    @Test
    fun `Given the update lambda throws When data is updated Then Unknown failure is returned`() = runTest {
        val result = dataSource.updateEngagementData { throw IllegalStateException("boom") }

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
        coVerify(exactly = 0) { engagementDao.replace(any(), any()) }
    }

    @Test
    fun `Given migration succeeds When data is observed Then stored data is emitted`() = runTest {
        val emissions = dataSource.observeEngagementData().toList()

        assertEquals(listOf(Answer.Success(storedData)), emissions)
    }

    @Test
    fun `Given migration fails When data is observed Then the failure is emitted followed by stored data`() = runTest {
        coEvery { legacyEngagementDataMigrator.migrate() } returns Answer.Failure(failure)

        val emissions = dataSource.observeEngagementData().toList()

        assertEquals(listOf(Answer.Failure(failure), Answer.Success(storedData)), emissions)
    }

    @Test
    fun `Given the dao throws When data is observed Then Unknown failure is emitted`() = runTest {
        coEvery { engagementDao.getState() } throws IllegalStateException("boom")

        val emissions = dataSource.observeEngagementData().toList()

        assertEquals(1, emissions.size)
        assertTrue((emissions.single() as Answer.Failure).value is Failure.Unknown)
    }
}
