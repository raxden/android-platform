package com.raxdenstudios.platform.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.raxdenstudios.commons.android.compose.model.ImageResource
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.commons.android.compose.preview.DevicePreviews
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.PreviewTheme
import com.raxdenstudios.platform.ui.theme.Spacing

/** Material top app bars with a one-line `HeadlineSmall` title. */
@OptIn(ExperimentalMaterial3Api::class)
object TopBar {

    /**
     * A top bar with an [IconButton.Back] tinted with [TopAppBarColors.navigationIconContentColor] (primary by
     * default).
     *
     * @param title optional one-line title; the bar shows only the back button when null.
     * @param backIcon icon of the back button, the small [Icons.BackArrow] chevron by default.
     * @param colors colours of the bar; its navigation icon colour tints the back button.
     * @param windowInsets insets the bar pads itself with, [TopAppBarDefaults.windowInsets] by default.
     * @param backButtonModifier applies to the back button only, e.g. a test tag.
     * @param onNavigationIconClick called when the back button is clicked.
     */
    @Composable
    fun Back(
        modifier: Modifier = Modifier,
        title: StringResource? = null,
        backIcon: ImageResource = Icons.BackArrow,
        colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
        ),
        windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
        backButtonModifier: Modifier = Modifier,
        onNavigationIconClick: () -> Unit,
    ) {
        TopAppBar(
            modifier = modifier.fillMaxWidth(),
            title = { title?.let { NavigationTitle(title) } },
            colors = colors,
            windowInsets = windowInsets,
            navigationIcon = {
                IconButton.Back(
                    modifier = backButtonModifier,
                    icon = backIcon,
                    contentColor = colors.navigationIconContentColor,
                    onClick = onNavigationIconClick,
                )
            },
        )
    }

    /**
     * A top bar with an optional [description] under the [title].
     *
     * @param title optional one-line title.
     * @param description optional `BodyMedium` line under the [title].
     * @param colors colours of the bar, [TopAppBarDefaults.topAppBarColors] by default; pass a `copy` with another
     * `containerColor` for a tinted bar.
     * @param windowInsets insets the bar pads itself with, [TopAppBarDefaults.windowInsets] by default.
     * @param actions icon buttons placed at the end of the bar.
     * @param navigationIcon content placed at the start of the bar, such as [IconButton.Back]; empty by default.
     */
    @Composable
    fun Default(
        modifier: Modifier = Modifier,
        title: StringResource? = null,
        description: StringResource? = null,
        colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
        windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
        actions: @Composable RowScope.() -> Unit = {},
        navigationIcon: @Composable () -> Unit = {},
    ) {
        TopAppBar(
            modifier = modifier.fillMaxWidth(),
            title = {
                Column {
                    title?.let { NavigationTitle(title) }
                    description?.let { Label.BodyMedium(it) }
                }
            },
            colors = colors,
            windowInsets = windowInsets,
            actions = actions,
            navigationIcon = navigationIcon,
        )
    }

    @Composable
    private fun NavigationTitle(
        title: StringResource,
    ) {
        Label.HeadlineSmall(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = Spacing.large),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
internal fun TopBarPreview() {
    PreviewTheme {
        TopBar.Back(onNavigationIconClick = {})
        TopBar.Back(
            title = StringResource.PlainText("Title"),
            backIcon = Icons.ArrowBack,
            onNavigationIconClick = {},
        )
        TopBar.Default(
            title = StringResource.PlainText("Title"),
            description = StringResource.PlainText("Description"),
            actions = { IconButton.Close(onClick = {}) },
        )
        TopBar.Default(
            title = StringResource.PlainText("Primary container"),
            colors = TopAppBarDefaults.topAppBarColors()
                .copy(containerColor = MaterialTheme.colorScheme.primaryContainer),
            navigationIcon = { IconButton.Back(onClick = {}) },
        )
    }
}
