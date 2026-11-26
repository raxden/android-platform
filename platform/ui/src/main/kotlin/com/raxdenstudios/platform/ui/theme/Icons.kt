package com.raxdenstudios.platform.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import com.raxdenstudios.commons.android.compose.model.ImageResource
import com.raxdenstudios.platform.ui.R

/**
 * Icons shared by the platform composables. Entries are [ImageResource]s, so [ImageResource.Vector] icons can be
 * tinted (see `EmptyState.Default` `tintVectors`) while [ImageResource.Drawable] icons keep their own colours.
 */
object Icons {

    val ChevronLeft = ImageResource.Vector(Icons.Filled.ChevronLeft)
    val ChevronRight = ImageResource.Vector(Icons.Filled.ChevronRight)
    val LocationOn = ImageResource.Vector(Icons.Default.LocationOn)

    /** Small 8x13dp chevron used by the default back buttons. */
    val BackArrow = ImageResource.Drawable(R.drawable.platform_ui_ic_back_arrow)

    /** Material arrow back, mirrored in right-to-left layouts. */
    val ArrowBack = ImageResource.Vector(Icons.AutoMirrored.Filled.ArrowBack)

    /** Material close cross, used by `IconButton.Close`. */
    val Close = ImageResource.Vector(Icons.Default.Close)

    /** 24dp double chevron used as the leading mark of `Header.Section`. */
    val DoubleArrow = ImageResource.Drawable(R.drawable.platform_ui_ic_double_arrow)

    /** Material arrow pointing right, mirrored in right-to-left layouts; trails `Button.TextArrow`. */
    val KeyboardArrowRight = ImageResource.Vector(Icons.AutoMirrored.Filled.KeyboardArrowRight)
}
