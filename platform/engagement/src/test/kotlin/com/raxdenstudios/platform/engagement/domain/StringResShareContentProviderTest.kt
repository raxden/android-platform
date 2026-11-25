package com.raxdenstudios.platform.engagement.domain

import android.content.Context
import com.raxdenstudios.platform.device.inapp.InAppShareContent
import com.raxdenstudios.platform.engagement.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

internal class StringResShareContentProviderTest {

    private val context: Context = mockk()

    private val provider = StringResShareContentProvider(
        context = context,
        subject = R.string.engagement_share_subject,
        text = R.string.engagement_share_text,
    )

    @Test
    fun `Given release package When provide Then subject and text come from the string resources`() {
        stubStrings(packageName = "com.app")

        val content = provider.provide()

        assertEquals(
            InAppShareContent(
                subject = "Subject",
                text = "Download https://play.google.com/store/apps/details?id=com.app",
            ),
            content,
        )
    }

    @Test
    fun `Given debug package When provide Then text is formatted with the release package name`() {
        stubStrings(packageName = "com.app.debug")

        val content = provider.provide()

        assertEquals(
            "Download https://play.google.com/store/apps/details?id=com.app",
            content.text,
        )
    }

    private fun stubStrings(
        packageName: String,
    ) {
        every { context.packageName } returns packageName
        every { context.getString(R.string.engagement_share_subject) } returns "Subject"
        every {
            context.getString(R.string.engagement_share_text, "com.app")
        } returns "Download https://play.google.com/store/apps/details?id=com.app"
    }
}
