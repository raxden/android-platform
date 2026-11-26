package com.raxdenstudios.platform.featureflag.data.repository

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.featureflag.data.datasource.remote.FeatureFlagRemoteDataSource
import com.raxdenstudios.platform.featureflag.domain.model.TestFeatureFlag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FeatureFlagRepositoryImplTest {

    private val remoteDataSource: FeatureFlagRemoteDataSource = mockk()
    private val repository = FeatureFlagRepositoryImpl(remoteDataSource = remoteDataSource)

    @Test
    fun `Given the data source succeeds When fetch Then delegates and returns success`() = runTest {
        coEvery { remoteDataSource.fetchAndActivate() } returns Answer.Success(Unit)

        assertEquals(Answer.Success(Unit), repository.fetch())
        coVerify(exactly = 1) { remoteDataSource.fetchAndActivate() }
    }

    @Test
    fun `Given the data source fails When fetch Then returns the same failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { remoteDataSource.fetchAndActivate() } returns failure

        assertEquals(failure, repository.fetch())
    }

    @Test
    fun `Given the data source answers a value When isEnabled Then delegates and returns it`() = runTest {
        coEvery { remoteDataSource.isEnabled(TestFeatureFlag.PODCASTS) } returns Answer.Success(true)

        assertEquals(Answer.Success(true), repository.isEnabled(TestFeatureFlag.PODCASTS))
        coVerify(exactly = 1) { remoteDataSource.isEnabled(TestFeatureFlag.PODCASTS) }
    }

    @Test
    fun `Given the data source fails When isEnabled Then returns the same failure`() = runTest {
        val failure = Answer.Failure(mockk<Failure>())
        coEvery { remoteDataSource.isEnabled(any()) } returns failure

        assertEquals(failure, repository.isEnabled(TestFeatureFlag.PODCASTS))
    }
}
