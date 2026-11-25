package com.raxdenstudios.platform.device.initializer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class CrashlyticsInitializerTest {

    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk()
    private val context: Context = mockk {
        every { packageName } returns PACKAGE_NAME
        every { this@mockk.packageManager } returns this@CrashlyticsInitializerTest.packageManager
    }
    private val initializer = CrashlyticsInitializer(crashlytics = { crashlytics })

    @Test
    fun `Given package info with version When create Then sets version name and code keys`() {
        stubPackageInfo(versionName = "1.2.3", versionCode = 42)

        initializer.create(context)

        verify(exactly = 1) { crashlytics.setCustomKey("app_version", "1.2.3") }
        verify(exactly = 1) { crashlytics.setCustomKey("app_version_code", 42) }
    }

    @Test
    fun `Given null version name When create Then sets empty version name`() {
        stubPackageInfo(versionName = null, versionCode = 7)

        initializer.create(context)

        verify(exactly = 1) { crashlytics.setCustomKey("app_version", "") }
        verify(exactly = 1) { crashlytics.setCustomKey("app_version_code", 7) }
    }

    @Test
    fun `When dependencies Then is empty`() {
        assertThat(initializer.dependencies()).isEmpty()
    }

    @Suppress("DEPRECATION")
    private fun stubPackageInfo(
        versionName: String?,
        versionCode: Int,
    ) {
        val info = mockk<PackageInfo>().apply {
            this.versionName = versionName
            this.versionCode = versionCode
        }
        every { packageManager.getPackageInfo(PACKAGE_NAME, 0) } returns info
    }

    private companion object {
        const val PACKAGE_NAME = "com.app"
    }
}
