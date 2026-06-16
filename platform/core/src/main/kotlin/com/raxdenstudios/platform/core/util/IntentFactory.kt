package com.raxdenstudios.platform.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import com.raxdenstudios.commons.core.ext.EMPTY

interface IntentFactory {

    fun build(type: IntentType): Intent
}

class IntentFactoryImpl(
    private val context: Context,
) : IntentFactory {

    override fun build(type: IntentType): Intent {
        return when (type) {
            is IntentType.AppSettings -> buildAppSettingsIntent(context)
            is IntentType.PlayStore -> buildPlayStoreIntent(context)
            is IntentType.ShareText -> buildShareTextIntent(type)
        }
    }

    private fun buildAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun buildPlayStoreIntent(
        context: Context,
    ): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            "market://details?id=${getReleasePackageName(context)}".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun buildShareTextIntent(
        intentType: IntentType.ShareText,
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, intentType.text)
            putExtra(Intent.EXTRA_SUBJECT, intentType.subject)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getReleasePackageName(context: Context): String {
        return context.packageName.replace(".debug", String.EMPTY)
    }
}
