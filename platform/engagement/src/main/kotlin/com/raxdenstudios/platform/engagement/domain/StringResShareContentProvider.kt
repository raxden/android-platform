package com.raxdenstudios.platform.engagement.domain

import android.content.Context
import androidx.annotation.StringRes
import com.raxdenstudios.commons.android.ext.releasePackageName
import com.raxdenstudios.platform.device.inapp.InAppShareContent

/**
 * [ShareContentProvider] that builds the share message from two app string resources, so an app
 * only declares its string ids:
 *
 * ```kotlin
 * module {
 *     factory<ShareContentProvider> {
 *         StringResShareContentProvider(
 *             context = androidContext(),
 *             subject = R.string.share_subject,
 *             text = R.string.share_text,
 *         )
 *     }
 * }
 * ```
 *
 * @param subject string used verbatim as the share subject; it takes no format arguments.
 * @param text string formatted with the release package name (the application id without the
 *   debug suffix, see `releasePackageName()`) as its only argument, typically appended to the Play
 *   Store URL: `"Download it now! https://play.google.com/store/apps/details?id=%s"`.
 */
class StringResShareContentProvider(
    private val context: Context,
    @param:StringRes private val subject: Int,
    @param:StringRes private val text: Int,
) : ShareContentProvider {

    override fun provide(): InAppShareContent = InAppShareContent(
        subject = context.getString(subject),
        text = context.getString(
            text,
            context.releasePackageName(),
        ),
    )
}
