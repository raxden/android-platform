package com.raxdenstudios.platform.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi preview annotation that represents various device sizes. Add this annotation to a composable
 * to render various devices.
 */
@Preview(
    name = "phone",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "phone-dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class DevicePreviews
