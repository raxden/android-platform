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
import com.raxdenstudios.platform.engagement.data.datasource.local.mapper.EngagementEntityDataMapper
import com.raxdenstudios.platform.engagement.domain.model.EngagementData
import com.raxdenstudios.platform.engagement.domain.model.ReviewState
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
    private val engagementEntityDataMapper: EngagementEntityDataMapper = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val stateEntity = EngagementStateEntity(
        reviewAttempts = 0,
        reviewLastAt = null,
        reviewRatedAt = null,
        shareDeclines = 0,
        shareLastPromptAt = null,
        shareSharedAt = null,
        pendingPromptAt = null,
    )
    private val ratedStateEntity = stateEntity.copy(reviewRatedAt = 1L)
    private val originCountEntities = listOf(EngagementOriginCountEntity(origin = "app_opened", occurrences = 3))
    private val storedData = EngagementData(counts = mapOf("app_opened" to 3))
    private val ratedData = storedData.copy(review = ReviewState(ratedAt = 1L))

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
        coEvery { engagementDao.getState() } returns stateEntity
        coEvery { engagementDao.getOriginCounts() } returns originCountEntities
        coEvery { engagementDao.upsertState(any()) } returns Unit
        coEvery { engagementDao.upsertOriginCounts(any()) } returns Unit
        coEvery { engagementDao.deleteOriginCounts(any()) } returns Unit
        every { engagementEntityDataMapper.toEngagementData(any(), any()) } returns storedData
        every { engagementEntityDataMapper.toStateEntity(storedData) } returns stateEntity
        every { engagementEntityDataMapper.toStateEntity(ratedData) } returns ratedStateEntity
        every { engagementEntityDataMapper.toOriginCountEntities(any()) } answers {
            firstArg<Map<String, Int>>().map { (origin, occurrences) ->
                EngagementOriginCountEntity(origin = origin, occurrences = occurrences)
            }
        }

        dataSource = EngagementDataLocalDataSourceImpl(
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
    fun `Given stored data When data is read Then the mapped data is returned`() = runTest {
        val result = dataSource.getEngagementData()

        assertEquals(Answer.Success(storedData), result)
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
    fun `Given the dao throws When data is read Then Unknown failure is returned`() = runTest {
        coEvery { engagementDao.getState() } throws IllegalStateException("boom")

        val result = dataSource.getEngagementData()

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
    }

    @Test
    fun `Given an update that changes the state When applied Then only the state is written`() = runTest {
        var received: EngagementData? = null

        val result = dataSource.updateEngagementData { current ->
            received = current
            ratedData
        }

        assertEquals(Answer.Success(ratedData), result)
        assertEquals(storedData, received)
        coVerify(exactly = 1) { engagementDao.upsertState(ratedStateEntity) }
        coVerify(exactly = 0) { engagementDao.upsertOriginCounts(any()) }
        coVerify(exactly = 0) { engagementDao.deleteOriginCounts(any()) }
    }

    @Test
    fun `Given an update that changes nothing When applied Then nothing is written`() = runTest {
        val result = dataSource.updateEngagementData { it }

        assertEquals(Answer.Success(storedData), result)
        coVerify(exactly = 0) { engagementDao.upsertState(any()) }
        coVerify(exactly = 0) { engagementDao.upsertOriginCounts(any()) }
        coVerify(exactly = 0) { engagementDao.deleteOriginCounts(any()) }
    }

    @Test
    fun `Given an update that only changes counts When applied Then the state is not rewritten`() = runTest {
        val updated = storedData.copy(counts = mapOf("app_opened" to 4))
        every { engagementEntityDataMapper.toStateEntity(updated) } returns stateEntity

        dataSource.updateEngagementData { updated }

        coVerify(exactly = 0) { engagementDao.upsertState(any()) }
        coVerify(exactly = 1) {
            engagementDao.upsertOriginCounts(
                listOf(EngagementOriginCountEntity(origin = "app_opened", occurrences = 4)),
            )
        }
        coVerify(exactly = 0) { engagementDao.deleteOriginCounts(any()) }
    }

    @Test
    fun `Given an update that changes one of two origins When applied Then only the changed origin is upserted`() =
        runTest {
            val current = EngagementData(counts = mapOf("app_opened" to 3, "favourite_added" to 1))
            every { engagementEntityDataMapper.toEngagementData(any(), any()) } returns current
            val updated = current.copy(counts = mapOf("app_opened" to 3, "favourite_added" to 2))
            every { engagementEntityDataMapper.toStateEntity(updated) } returns stateEntity

            dataSource.updateEngagementData { updated }

            coVerify(exactly = 1) {
                engagementDao.upsertOriginCounts(
                    listOf(EngagementOriginCountEntity(origin = "favourite_added", occurrences = 2)),
                )
            }
        }

    @Test
    fun `Given an update that adds an origin When applied Then the new origin is upserted`() = runTest {
        val updated = storedData.copy(counts = mapOf("app_opened" to 3, "new_origin" to 1))
        every { engagementEntityDataMapper.toStateEntity(updated) } returns stateEntity

        dataSource.updateEngagementData { updated }

        coVerify(exactly = 1) {
            engagementDao.upsertOriginCounts(
                listOf(EngagementOriginCountEntity(origin = "new_origin", occurrences = 1)),
            )
        }
    }

    @Test
    fun `Given an update that removes an origin When applied Then the removed origin is deleted`() = runTest {
        val updated = storedData.copy(counts = emptyMap())
        every { engagementEntityDataMapper.toStateEntity(updated) } returns stateEntity

        dataSource.updateEngagementData { updated }

        coVerify(exactly = 1) { engagementDao.deleteOriginCounts(listOf("app_opened")) }
        coVerify(exactly = 0) { engagementDao.upsertOriginCounts(any()) }
        coVerify(exactly = 0) { engagementDao.upsertState(any()) }
    }

    @Test
    fun `Given no stored state row When an update changes data Then the state is inserted`() = runTest {
        coEvery { engagementDao.getState() } returns null
        every { engagementEntityDataMapper.toEngagementData(null, any()) } returns storedData
        val updated = storedData.copy(counts = mapOf("app_opened" to 4))
        every { engagementEntityDataMapper.toStateEntity(updated) } returns stateEntity

        dataSource.updateEngagementData { updated }

        coVerify(exactly = 1) { engagementDao.upsertState(stateEntity) }
    }

    @Test
    fun `Given the dao throws on write When data is updated Then Unknown failure is returned`() = runTest {
        coEvery { engagementDao.upsertState(any()) } throws IllegalStateException("boom")

        val result = dataSource.updateEngagementData { ratedData }

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
    }

    @Test
    fun `Given the update lambda throws When data is updated Then Unknown failure is returned`() = runTest {
        val result = dataSource.updateEngagementData { throw IllegalStateException("boom") }

        assertTrue(result is Answer.Failure)
        assertTrue((result as Answer.Failure).value is Failure.Unknown)
        coVerify(exactly = 0) { engagementDao.upsertState(any()) }
        coVerify(exactly = 0) { engagementDao.upsertOriginCounts(any()) }
        coVerify(exactly = 0) { engagementDao.deleteOriginCounts(any()) }
    }

    @Test
    fun `Given stored data When data is observed Then stored data is emitted`() = runTest {
        val emissions = dataSource.observeEngagementData().toList()

        assertEquals(listOf(Answer.Success(storedData)), emissions)
    }

    @Test
    fun `Given the dao throws When data is observed Then Unknown failure is emitted`() = runTest {
        coEvery { engagementDao.getState() } throws IllegalStateException("boom")

        val emissions = dataSource.observeEngagementData().toList()

        assertEquals(1, emissions.size)
        assertTrue((emissions.single() as Answer.Failure).value is Failure.Unknown)
    }
}
