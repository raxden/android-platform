package com.raxdenstudios.platform.device.inapp

import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.raxdenstudios.platform.device.R
import kotlinx.coroutines.CompletableDeferred

/**
 * Native modal asking the user whether they want to share the app, shown before the system share
 * sheet by [InAppShare.launch].
 *
 * Rendered as a Material 3 bottom sheet that mimics the header/title/message/actions layout of
 * the Play Store in-app review card. The header app icon and name come from the package manager,
 * while the title, message and both action labels are read from the `in_app_share_prompt_title`,
 * `in_app_share_prompt_message`, `in_app_share_prompt_positive` and `in_app_share_prompt_negative`
 * string resources, so host apps can override the copy in their own `strings.xml`.
 */
internal interface InAppSharePrompt {

    /**
     * Shows the modal on [activity] and suspends until it is answered or dismissed.
     *
     * Returns `true` when the user taps the positive action (Share). Returns `false` when they
     * tap the negative action (Not now), press back, tap outside the sheet or the activity is
     * destroyed while the modal is visible. If the awaiting coroutine is cancelled the dialog is
     * still dismissed and its lifecycle observer removed before the cancellation propagates.
     *
     * Must be called on the main thread.
     */
    suspend fun show(activity: ComponentActivity): Boolean
}

/**
 * Bridges the [BottomSheetDialog] callbacks into a [CompletableDeferred] and completes it with
 * `false` from a [DefaultLifecycleObserver] when the activity is destroyed, so the suspended
 * caller never waits forever on a dialog that can no longer be answered.
 */
internal class InAppSharePromptImpl : InAppSharePrompt {

    override suspend fun show(activity: ComponentActivity): Boolean {
        val answer = CompletableDeferred<Boolean>()
        val dialog = buildDialog(activity, answer)
        val observer = closeOnDestroy(dialog, answer)
        activity.lifecycle.addObserver(observer)
        dialog.show()
        return try {
            answer.await()
        } finally {
            activity.lifecycle.removeObserver(observer)
            dialog.dismiss()
        }
    }

    private fun buildDialog(
        activity: ComponentActivity,
        answer: CompletableDeferred<Boolean>,
    ): BottomSheetDialog {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.in_app_share_prompt, null)
        bindContent(activity, view)
        bindActions(dialog, view, answer)
        dialog.setContentView(view)
        dialog.setOnCancelListener { answer.complete(false) }
        return dialog
    }

    private fun bindContent(activity: ComponentActivity, view: android.view.View) {
        val packageManager = activity.packageManager
        val applicationInfo = activity.applicationInfo
        val appName = applicationInfo.loadLabel(packageManager)
        view.findViewById<ImageView>(R.id.in_app_share_prompt_icon).setImageDrawable(
            packageManager.getApplicationIcon(applicationInfo.packageName),
        )
        view.findViewById<TextView>(R.id.in_app_share_prompt_app_name).text = appName
        view.findViewById<TextView>(R.id.in_app_share_prompt_title).text =
            activity.getString(R.string.in_app_share_prompt_title, appName)
        view.findViewById<TextView>(R.id.in_app_share_prompt_message).text =
            activity.getString(R.string.in_app_share_prompt_message)
    }

    private fun bindActions(
        dialog: BottomSheetDialog,
        view: android.view.View,
        answer: CompletableDeferred<Boolean>,
    ) {
        view.findViewById<MaterialButton>(R.id.in_app_share_prompt_positive).setOnClickListener {
            answer.complete(true)
            dialog.dismiss()
        }
        view.findViewById<MaterialButton>(R.id.in_app_share_prompt_negative).setOnClickListener {
            answer.complete(false)
            dialog.dismiss()
        }
    }

    private fun closeOnDestroy(
        dialog: BottomSheetDialog,
        answer: CompletableDeferred<Boolean>,
    ): DefaultLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            answer.complete(false)
            dialog.dismiss()
        }
    }
}
