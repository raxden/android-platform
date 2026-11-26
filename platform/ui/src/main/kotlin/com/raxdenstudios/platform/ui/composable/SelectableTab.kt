package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.raxdenstudios.platform.ui.R

/**
 * Makes this element a tab that stays clickable while selected. Compose reports a selected `Role.Tab` (as
 * `Modifier.selectable` and Material's `Tab` set it) as not clickable, and UiAutomator then loses it together with
 * its test tag. Here the selection reaches TalkBack as the state description ("Selected") instead, and tapping the
 * selected tab does nothing.
 */
@Composable
fun Modifier.selectableTab(
    isSelected: Boolean,
    onClick: () -> Unit,
): Modifier {
    val selectedDescription = stringResource(R.string.platform_ui_tab_selected)
    return clickable(role = Role.Tab) { if (isSelected.not()) onClick() }
        .semantics { if (isSelected) stateDescription = selectedDescription }
}
