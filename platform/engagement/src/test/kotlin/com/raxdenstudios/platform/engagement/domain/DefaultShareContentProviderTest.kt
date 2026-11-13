package com.raxdenstudios.platform.engagement.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.raxdenstudios.platform.device.inapp.InAppShareContent
import com.raxdenstudios.platform.engagement.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class DefaultShareContentProviderTest {

    private val packageManager: PackageManager = mockk()
    private val applicationInfo: ApplicationInfo = mockk()
    private val context: Context = mockk()

    private lateinit var provider: DefaultShareContentProvider

    @Before
    fun setUp() {
        every { context.packageName } returns "com.raxdenstudios.app"
        every { context.packageManager } returns packageManager
        every { context.applicationInfo } returns applicationInfo
        every { applicationInfo.loadLabel(packageManager) } returns "My App"
        every { context.getString(R.string.engagement_share_subject, "My App") } returns "Check out My App!"
        every {
            context.getString(
                R.string.engagement_share_text,
                "My App",
                "https://play.google.com/store/apps/details?id=com.raxdenstudios.app",
            )
        } returns
            "I'm using My App. Download it now! https://play.google.com/store/apps/details?id=com.raxdenstudios.app"

        provider = DefaultShareContentProvider(context)
    }

    @Test
    fun `Given app context When provide Then content is built from the app name and its store link`() {
        val content = provider.provide()

        assertEquals(
            InAppShareContent(
                subject = "Check out My App!",
                text = "I'm using My App. Download it now! " +
                    "https://play.google.com/store/apps/details?id=com.raxdenstudios.app",
            ),
            content,
        )
    }
}
