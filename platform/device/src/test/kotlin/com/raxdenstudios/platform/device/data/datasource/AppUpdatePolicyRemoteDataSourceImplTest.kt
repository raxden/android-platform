package com.raxdenstudios.platform.device.data.datasource

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.domain.model.AppUpdatePolicy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.hours

internal class AppUpdatePolicyRemoteDataSourceImplTest {

    private val remoteConfig: FirebaseRemoteConfig = mockk()
    private val dataSource = AppUpdatePolicyRemoteDataSourceImpl(remoteConfig, fetchInterval = 12.hours)

    @Test
    fun `Given refresh succeeds When getPolicy Then fetches with interval and returns policy`() = runTest {
        every { remoteConfig.fetch(any()) } returns completedTask<Void>()
        every { remoteConfig.activate() } returns completedTask(true)
        stubKeys(min = 100L, recommended = 110L)

        val result = dataSource.getPolicy()

        assertThat(result).isEqualTo(Answer.Success(AppUpdatePolicy.mock))
        verify { remoteConfig.fetch(12L * 3600L) }
        verify { remoteConfig.activate() }
    }

    @Test
    fun `Given fetch fails When getPolicy Then still activates and returns last values`() = runTest {
        every { remoteConfig.fetch(any()) } returns completedTask<Void>(error = Exception("offline"))
        every { remoteConfig.activate() } returns completedTask(false)
        stubKeys(min = 5L, recommended = 6L)

        val result = dataSource.getPolicy()

        assertThat(result).isEqualTo(Answer.Success(AppUpdatePolicy(5L, 6L)))
        verify { remoteConfig.activate() }
    }

    @Test
    fun `Given activate fails When getPolicy Then returns values read`() = runTest {
        every { remoteConfig.fetch(any()) } returns completedTask<Void>()
        every { remoteConfig.activate() } returns completedTask<Boolean>(error = Exception("boom"))
        stubKeys(min = 0L, recommended = 0L)

        val result = dataSource.getPolicy()

        assertThat(result).isEqualTo(Answer.Success(AppUpdatePolicy(0L, 0L)))
    }

    @Test
    fun `Given reading a key throws When getPolicy Then returns unknown failure`() = runTest {
        every { remoteConfig.fetch(any()) } returns completedTask<Void>()
        every { remoteConfig.activate() } returns completedTask(true)
        every { remoteConfig.getLong(any()) } throws IllegalStateException("boom")

        val result = dataSource.getPolicy()

        assertThat(result).isInstanceOf(Answer.Failure::class.java)
        val (failure) = result as Answer.Failure<Failure>
        assertThat(failure).isInstanceOf(Failure.Unknown::class.java)
    }

    private fun stubKeys(
        min: Long,
        recommended: Long,
    ) {
        every { remoteConfig.getLong(AppUpdatePolicyRemoteDataSource.MIN_VERSION_CODE_KEY) } returns min
        every { remoteConfig.getLong(AppUpdatePolicyRemoteDataSource.RECOMMENDED_VERSION_CODE_KEY) } returns recommended
    }

    /**
     * A [Task] already completed whose listeners run inline: `awaitAnswer` registers through
     * `addOnCompleteListener`, whose executor needs the main Looper that local unit tests lack.
     */
    private fun <T> completedTask(
        value: T? = null,
        error: Exception? = null,
    ): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns (error == null)
        every { task.isCanceled } returns false
        every { task.exception } returns error
        every { task.result } returns value
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(task)
            task
        }
        return task
    }
}
