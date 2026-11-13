package com.raxdenstudios.platform.engagement.domain

import android.content.Context
import com.raxdenstudios.commons.android.provider.StringProvider
import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.commons.core.ext.EMPTY
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShare
import com.raxdenstudios.platform.device.inapp.InAppShareContent
import com.raxdenstudios.platform.device.inapp.InAppShareResult
import com.raxdenstudios.platform.engagement.R

/**
 * Shares a promotional message about the app, built from
 * [R.string.account_share_text_subject] and [R.string.account_share_text_value].
 *
 * With [Params.withPrompt] set, [InAppShare.launch] first shows a native modal asking the user
 * whether they want to share and only opens the system share sheet on acceptance; otherwise
 * [InAppShare.share] opens the sheet directly.
 */
interface ShareAppUseCase : SuspendUseCase<ShareAppUseCase.Params, InAppShareResult> {

    data class Params(val withPrompt: Boolean = false)
}

class ShareAppUseCaseImpl(
    private val context: Context,
    private val stringProvider: StringProvider,
    private val inAppShare: InAppShare,
) : ShareAppUseCase {

    override suspend fun invoke(params: ShareAppUseCase.Params): Answer<InAppShareResult, Failure> =
        share(params.withPrompt)

    private suspend fun share(withPrompt: Boolean): Answer<InAppShareResult, Failure> {
        val content = buildShareContent()
        return if (withPrompt) inAppShare.launch(content) else inAppShare.share(content)
    }

    private fun buildShareContent(): InAppShareContent = InAppShareContent(
        subject = stringProvider.getString(R.string.account_share_text_subject),
        text = stringProvider.getString(
            R.string.account_share_text_value,
            context.packageName.replace(".debug", String.EMPTY),
        ),
    )
}
