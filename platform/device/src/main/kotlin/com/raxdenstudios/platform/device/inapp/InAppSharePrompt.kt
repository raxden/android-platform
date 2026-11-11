package com.raxdenstudios.platform.device.inapp

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.raxdenstudios.platform.device.R
import com.raxdenstudios.platform.ui.composable.BottomSheet
import com.raxdenstudios.platform.ui.composable.Label
import com.raxdenstudios.platform.ui.composable.Spacer
import com.raxdenstudios.platform.ui.model.StringResource.PlainText
import com.raxdenstudios.platform.ui.model.StringResource.ResourceId
import com.raxdenstudios.platform.ui.theme.ThemeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * Native modal asking the user whether they want to share the app, shown before the system share
 * sheet by [InAppShare.launch].
 *
 * Rendered as a Material 3 [BottomSheet.Simple] that mimics the header/title/message/actions
 * layout of the Play Store in-app review card. The header app icon and name come from the
 * package manager, while the title, message and both action labels are read from the
 * `in_app_share_prompt_title`, `in_app_share_prompt_message`, `in_app_share_prompt_positive` and
 * `in_app_share_prompt_negative` string resources, so host apps can override the copy in their
 * own `strings.xml`.
 */
internal interface InAppSharePrompt {

    /**
     * Shows the modal on [activity] and suspends until it is answered or dismissed.
     *
     * Returns `true` when the user taps the positive action (Share). Returns `false` when they
     * tap the negative action (Not now), press back, tap outside the sheet or the activity is
     * destroyed while the modal is visible. If the awaiting coroutine is cancelled the sheet is
     * still dismissed and its lifecycle observer removed before the cancellation propagates.
     *
     * Must be called on the main thread.
     */
    suspend fun show(activity: ComponentActivity): Boolean
}

/**
 * Hosts [BottomSheet.Simple] on a standalone [ComposeView] attached to the activity decor view,
 * since [InAppSharePrompt] is shown from a context without its own `setContent`. The sheet is
 * wrapped in the [ThemeProvider] registered in DI — `DefaultThemeProvider` by default, or the
 * provider the host app overrides it with — so it follows the app palette and light/dark
 * preference instead of the stock Material 3 color scheme. The sheet answer is bridged into a
 * [CompletableDeferred] that also completes with `false` from a [DefaultLifecycleObserver] when
 * the activity is destroyed, so the suspended caller never waits forever on a sheet that can no
 * longer be answered.
 */
internal class InAppSharePromptImpl(
    private val themeProvider: ThemeProvider,
) : InAppSharePrompt {

    override suspend fun show(activity: ComponentActivity): Boolean {
        val answer = CompletableDeferred<Boolean>()
        val composeView = buildComposeView(activity, answer)
        val observer = closeOnDestroy(answer)
        activity.lifecycle.addObserver(observer)
        attach(activity, composeView)
        return try {
            answer.await()
        } finally {
            activity.lifecycle.removeObserver(observer)
            detach(activity, composeView)
        }
    }

    private fun buildComposeView(
        activity: ComponentActivity,
        answer: CompletableDeferred<Boolean>,
    ): ComposeView = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            themeProvider.Apply {
                InAppSharePromptSheet(
                    activity = activity,
                    onShare = { answer.complete(true) },
                    onDismiss = { answer.complete(false) },
                )
            }
        }
    }

    private fun attach(activity: ComponentActivity, composeView: ComposeView) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        content.addView(
            composeView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    private fun detach(activity: ComponentActivity, composeView: ComposeView) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        content.removeView(composeView)
    }

    private fun closeOnDestroy(
        answer: CompletableDeferred<Boolean>,
    ): DefaultLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            answer.complete(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppSharePromptSheet(
    activity: ComponentActivity,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun answerAndHide(onAnswered: () -> Unit) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) onAnswered()
        }
    }

    BottomSheet.Simple(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        primaryButton = ResourceId(R.string.in_app_share_prompt_positive) to {
            answerAndHide(onShare)
        },
        secondaryButton = ResourceId(R.string.in_app_share_prompt_negative) to {
            answerAndHide(onDismiss)
        },
    ) {
        InAppSharePromptHeader(activity)
        Spacer.Vertical.Large()
        val appName = activity.applicationInfo.loadLabel(activity.packageManager).toString()
        Label.TitleLarge(ResourceId(R.string.in_app_share_prompt_title, listOf(appName)))
        Spacer.Vertical.Small()
        Label.BodyMedium(ResourceId(R.string.in_app_share_prompt_message))
        Spacer.Vertical.Large()
    }
}

@Composable
private fun InAppSharePromptHeader(activity: ComponentActivity) {
    val packageManager = activity.packageManager
    val applicationInfo = activity.applicationInfo
    val appName = applicationInfo.loadLabel(packageManager).toString()
    val icon = packageManager.getApplicationIcon(applicationInfo.packageName)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer.Horizontal.Small()
        Label.LabelLarge(
            text = PlainText(appName),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
