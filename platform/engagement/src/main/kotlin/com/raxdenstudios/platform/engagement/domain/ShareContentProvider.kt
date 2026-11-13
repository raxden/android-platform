package com.raxdenstudios.platform.engagement.domain

import android.content.Context
import com.raxdenstudios.platform.device.inapp.InAppShareContent
import com.raxdenstudios.platform.engagement.R

/**
 * Supplies the promotional message used by [ShareAppUseCase]. [DefaultShareContentProvider] builds
 * a generic one from the app label and its Play Store link, formatted with the
 * `engagement_share_subject` and `engagement_share_text` strings — apps can override them by
 * resource name, or bind their own implementation via DI to customise the copy entirely.
 */
interface ShareContentProvider {

    fun provide(): InAppShareContent
}

internal class DefaultShareContentProvider(private val context: Context) : ShareContentProvider {

    override fun provide(): InAppShareContent {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        return InAppShareContent(
            subject = context.getString(R.string.engagement_share_subject, appName),
            text = context.getString(R.string.engagement_share_text, appName, playStoreUrl()),
        )
    }

    private fun playStoreUrl(): String = "$PLAY_STORE_URL${context.packageName}"

    private companion object {
        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id="
    }
}
