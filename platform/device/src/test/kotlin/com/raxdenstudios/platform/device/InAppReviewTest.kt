package com.raxdenstudios.platform.device

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.common.truth.Truth.assertThat
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.core.util.IntentFactoryImpl
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InAppReviewTest {

    private val context: Context = mockk {
        every { applicationContext } returns this
        every { packageName } returns "com.test"
    }
    private val reviewManager: ReviewManager = mockk()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given no activity attached When launch Then returns resource not found failure`() =
        runTest {
            val inAppReview = createReleaseInAppReview()

            val result = inAppReview.launch()

            assertThat(result).isEqualTo(
                Answer.Failure(Failure.ResourceNotFound("No activity attached")),
            )
        }

    @Test
    fun `Given no prefetched review info When launch Then requests review info and launches flow`() =
        runTest {
            val info: ReviewInfo = mockk()
            every { reviewManager.requestReviewFlow() } returns completedReviewTask(info)
            every { reviewManager.launchReviewFlow(any(), any()) } returns completedLaunchTask()
            val inAppReview = createReleaseInAppReview()
            val activity = attachActivity(inAppReview)

            val result = inAppReview.launch()

            assertThat(result).isEqualTo(Answer.Success(Unit))
            verify(exactly = 1) { reviewManager.requestReviewFlow() }
            verify { reviewManager.launchReviewFlow(activity, info) }
        }

    @Test
    fun `Given prefetched review info When launch Then cached info is launched without a new request`() =
        runTest {
            val info: ReviewInfo = mockk()
            every { reviewManager.requestReviewFlow() } returns completedReviewTask(info)
            every { reviewManager.launchReviewFlow(any(), any()) } returns completedLaunchTask()
            val inAppReview = createReleaseInAppReview()
            val activity = attachActivity(inAppReview)
            inAppReview.onCreate(activity)

            val result = inAppReview.launch()

            assertThat(result).isEqualTo(Answer.Success(Unit))
            verify(exactly = 1) { reviewManager.requestReviewFlow() }
            verify { reviewManager.launchReviewFlow(activity, info) }
        }

    @Test
    fun `Given prefetched review info consumed When launch again Then a fresh info is requested`() =
        runTest {
            val prefetched: ReviewInfo = mockk()
            val requested: ReviewInfo = mockk()
            every { reviewManager.requestReviewFlow() } returnsMany listOf(
                completedReviewTask(prefetched),
                completedReviewTask(requested),
            )
            every { reviewManager.launchReviewFlow(any(), any()) } returns completedLaunchTask()
            val inAppReview = createReleaseInAppReview()
            val activity = attachActivity(inAppReview)
            inAppReview.onCreate(activity)

            inAppReview.launch()
            inAppReview.launch()

            verify(exactly = 2) { reviewManager.requestReviewFlow() }
            verifyOrder {
                reviewManager.launchReviewFlow(activity, prefetched)
                reviewManager.launchReviewFlow(activity, requested)
            }
        }

    @Test
    fun `Given review request fails When launch Then returns unknown failure`() = runTest {
        every { reviewManager.requestReviewFlow() } returns failedReviewTask(Exception("boom"))
        val inAppReview = createReleaseInAppReview()
        attachActivity(inAppReview)

        val result = inAppReview.launch()

        assertThat(result).isEqualTo(
            Answer.Failure(
                Failure.Unknown("In-app review flow could not be launched: boom"),
            ),
        )
    }

    @Test
    fun `Given launch flow fails When launch Then returns unknown failure`() = runTest {
        every { reviewManager.requestReviewFlow() } returns completedReviewTask()
        every { reviewManager.launchReviewFlow(any(), any()) } returns
            failedLaunchTask(Exception("boom"))
        val inAppReview = createReleaseInAppReview()
        attachActivity(inAppReview)

        val result = inAppReview.launch()

        assertThat(result).isEqualTo(
            Answer.Failure(
                Failure.Unknown("In-app review flow could not be launched: boom"),
            ),
        )
    }

    @Test
    fun `Given debug review When launch Then opens the play store`() = runTest {
        val inAppReview = createDebugInAppReview()
        every { context.startActivity(any()) } just Runs

        val result = inAppReview.launch()

        assertThat(result).isEqualTo(Answer.Success(Unit))
        verify { context.startActivity(any()) }
    }

    @Test
    fun `Given debug review and play store cannot be opened When launch Then returns unknown failure`() =
        runTest {
            val inAppReview = createDebugInAppReview()
            every { context.startActivity(any()) } throws Exception("boom")

            val result = inAppReview.launch()

            assertThat(result).isEqualTo(
                Answer.Failure(
                    Failure.Unknown("Play Store could not be opened: boom"),
                ),
            )
        }

    private fun createReleaseInAppReview(): ReleaseInAppReview {
        mockkStatic(ReviewManagerFactory::class)
        every { ReviewManagerFactory.create(any()) } returns reviewManager
        return ReleaseInAppReview(context)
    }

    private fun createDebugInAppReview(): DebugInAppReview {
        mockkConstructor(IntentFactoryImpl::class)
        every { anyConstructed<IntentFactoryImpl>().build(any()) } returns mockk<Intent>()
        return DebugInAppReview(context)
    }

    private fun attachActivity(inAppReview: InAppReview): ComponentActivity {
        val lifecycle: Lifecycle = mockk()
        val activity: ComponentActivity = mockk()
        every { lifecycle.addObserver(any()) } just Runs
        every { lifecycle.removeObserver(any()) } just Runs
        every { activity.lifecycle } returns lifecycle
        inAppReview.attach(activity)
        return activity
    }

    /**
     * A [Task] completed with [info] whose listeners run inline. `Tasks.forResult` cannot be used
     * for the listener-based prefetch path because `addOnCompleteListener` posts through
     * `Handler(Looper.getMainLooper())`, which does not exist in local unit tests.
     */
    private fun completedReviewTask(info: ReviewInfo = mockk()): Task<ReviewInfo> {
        val task = mockk<Task<ReviewInfo>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns info
        every { task.addOnCompleteListener(any<OnCompleteListener<ReviewInfo>>()) } answers {
            firstArg<OnCompleteListener<ReviewInfo>>().onComplete(task)
            task
        }
        every { task.addOnSuccessListener(any<OnSuccessListener<ReviewInfo>>()) } answers {
            firstArg<OnSuccessListener<ReviewInfo>>().onSuccess(info)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun failedReviewTask(error: Exception): Task<ReviewInfo> {
        val task = mockk<Task<ReviewInfo>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.exception } returns error
        every { task.addOnCompleteListener(any<OnCompleteListener<ReviewInfo>>()) } answers {
            firstArg<OnCompleteListener<ReviewInfo>>().onComplete(task)
            task
        }
        every { task.addOnSuccessListener(any<OnSuccessListener<ReviewInfo>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun completedLaunchTask(): Task<Void> {
        val task = mockk<Task<Void>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } answers { null }
        return task
    }

    private fun failedLaunchTask(error: Exception): Task<Void> {
        val task = mockk<Task<Void>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.exception } returns error
        return task
    }
}
