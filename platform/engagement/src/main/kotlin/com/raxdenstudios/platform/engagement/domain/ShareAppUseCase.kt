package com.raxdenstudios.platform.engagement.domain

import com.raxdenstudios.commons.core.Answer
import com.raxdenstudios.platform.core.domain.SuspendUseCase
import com.raxdenstudios.platform.core.domain.model.Failure
import com.raxdenstudios.platform.device.inapp.InAppShare
import com.raxdenstudios.platform.device.inapp.InAppShareResult

/**
 * Shares the promotional message supplied by [ShareContentProvider].
 *
 * With [Params.withPrompt] set, [InAppShare.launch] first shows a native modal asking the user
 * whether they want to share and only opens the system share sheet on acceptance; otherwise
 * [InAppShare.share] opens the sheet directly.
 */
interface ShareAppUseCase : SuspendUseCase<ShareAppUseCase.Params, InAppShareResult> {

    data class Params(val withPrompt: Boolean = false)
}

internal class ShareAppUseCaseImpl(
    private val shareContentProvider: ShareContentProvider,
    private val inAppShare: InAppShare,
) : ShareAppUseCase {

    override suspend fun invoke(params: ShareAppUseCase.Params): Answer<InAppShareResult, Failure> =
        share(params.withPrompt)

    private suspend fun share(withPrompt: Boolean): Answer<InAppShareResult, Failure> {
        val content = shareContentProvider.provide()
        return if (withPrompt) inAppShare.launch(content) else inAppShare.share(content)
    }
}
