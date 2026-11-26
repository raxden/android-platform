package com.raxdenstudios.platform.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raxdenstudios.commons.android.compose.model.ImageResource
import com.raxdenstudios.commons.android.compose.model.StringResource
import com.raxdenstudios.commons.android.compose.preview.DevicePreviews
import com.raxdenstudios.platform.ui.motion.rememberReducedMotion
import com.raxdenstudios.platform.ui.theme.Icons
import com.raxdenstudios.platform.ui.theme.PreviewTheme

/**
 * Bottom navigation of the main tabs: a 64dp row of [BottomNavigationBarItem]s on the surface colour, under a thin
 * divider and above the system navigation bar.
 *
 * When [visible] turns off the bar sinks with [exit] and gives its space back at the start of the animation; when it
 * turns on it rises with [enter] and takes its space at once, so the screen above resizes once instead of every frame.
 * While the system has animations off (see `rememberReducedMotion`) [enter] and [exit] are replaced by a short fade,
 * and item colour and indicator changes snap.
 *
 * It is a slot API like Material 3's `NavigationBar`: [content] receives a `RowScope` where each tab is a
 * [BottomNavigationBarItem]. The caller owns the selection state and passes `selected` and `onClick` to every item.
 *
 * ## Usage
 *
 * ```kotlin
 * var selectedTab by remember { mutableStateOf(Tab.Home) }
 *
 * BottomNavigationBar(visible = showBottomBar) {
 *     BottomNavigationBarItem(
 *         label = StringResource.PlainText("Home"),
 *         icon = Icons.LocationOn,
 *         selected = selectedTab == Tab.Home,
 *         onClick = { selectedTab = Tab.Home },
 *     )
 *     BottomNavigationBarItem(
 *         label = StringResource.PlainText("Profile"),
 *         icon = Icons.ChevronRight,
 *         selected = selectedTab == Tab.Profile,
 *         badge = StringResource.PlainText("New"),
 *         onClick = { selectedTab = Tab.Profile },
 *     )
 * }
 * ```
 */
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    enter: EnterTransition = BottomNavigationBarDefaults.enterTransition(),
    exit: ExitTransition = BottomNavigationBarDefaults.exitTransition(),
    content: @Composable RowScope.() -> Unit,
) {
    val isReducedMotion = rememberReducedMotion()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.collapseWhenHidden(visible),
        enter = motionAware(isReducedMotion, enter, BottomNavigationBarDefaults.reducedMotionEnterTransition()),
        exit = motionAware(isReducedMotion, exit, BottomNavigationBarDefaults.reducedMotionExitTransition()),
        label = "bottomNavigationBar",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(
                thickness = BottomNavigationBarDefaults.DividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomNavigationBarDefaults.BarHeight)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceAround,
                content = content,
            )
        }
    }
}

/**
 * One tab of a [BottomNavigationBar]: a pill indicator, the icon and the label, primary when [selected] and
 * on-surface-variant otherwise. It stays clickable while selected (see [selectableTab]) and tapping it then does
 * nothing.
 *
 * @param selectedIcon icon shown while [selected]; [icon] by default.
 * @param badge optional text shown in a primary badge over the top-end corner of the icon, such as "New".
 * @param indicatorSpacing space between the indicator and the icon.
 * @param labelSpacing space between the icon and the label.
 * @param colorAnimationSpec timing of the colour change on selection; ignored (snaps) with reduced motion.
 * @param indicatorAnimationSpec timing of the indicator width change; ignored (snaps) with reduced motion.
 */
@Composable
fun RowScope.BottomNavigationBarItem(
    label: StringResource,
    icon: ImageResource,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: ImageResource = icon,
    badge: StringResource? = null,
    indicatorSpacing: Dp = BottomNavigationBarDefaults.IndicatorSpacing,
    labelSpacing: Dp = BottomNavigationBarDefaults.LabelSpacing,
    colorAnimationSpec: AnimationSpec<Color> = BottomNavigationBarDefaults.stateChangeSpec(),
    indicatorAnimationSpec: AnimationSpec<Dp> = BottomNavigationBarDefaults.stateChangeSpec(),
) {
    val isReducedMotion = rememberReducedMotion()
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motionAware(isReducedMotion, colorAnimationSpec, snap()),
        label = "bottomNavigationItemColor",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) BottomNavigationBarDefaults.IndicatorWidth else 0.dp,
        animationSpec = motionAware(isReducedMotion, indicatorAnimationSpec, snap()),
        label = "bottomNavigationItemIndicator",
    )

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .selectableTab(isSelected = selected, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(BottomNavigationBarDefaults.IndicatorHeight)
                .background(
                    color = if (selected) contentColor else Color.Transparent,
                    shape = RoundedCornerShape(BottomNavigationBarDefaults.IndicatorHeight),
                ),
        )
        Spacer.Vertical.Custom(indicatorSpacing)
        ItemIcon(
            icon = if (selected) selectedIcon else icon,
            tint = contentColor,
            badge = badge,
        )
        Spacer.Vertical.Custom(labelSpacing)
        Label.LabelMedium(
            text = label,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** Sizes and motion of [BottomNavigationBar] and [BottomNavigationBarItem]. */
object BottomNavigationBarDefaults {

    /** Height of the row of items, below the divider. */
    val BarHeight: Dp = 64.dp

    /** Thickness of the divider drawn above the items. */
    val DividerThickness: Dp = 0.5.dp

    /** Width of the pill indicator of a selected item. */
    val IndicatorWidth: Dp = 20.dp

    /** Height of the pill indicator of a selected item. */
    val IndicatorHeight: Dp = 3.dp

    /** Space between the indicator and the icon of an item. */
    val IndicatorSpacing: Dp = 4.dp

    /** Space between the icon and the label of an item. */
    val LabelSpacing: Dp = 2.dp

    private const val ENTER_DURATION_MILLIS = 300
    private const val EXIT_DURATION_MILLIS = 200
    private const val STATE_CHANGE_DURATION_MILLIS = 250
    private const val REDUCED_MOTION_FADE_MILLIS = 150

    private val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    private val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** The bar rising from the bottom edge with the Material emphasized decelerate easing. */
    fun enterTransition(): EnterTransition = slideInVertically(
        animationSpec = enterSpec(),
        initialOffsetY = { fullHeight -> fullHeight },
    ) + fadeIn(enterSpec())

    /** The bar sinking through the bottom edge, faster than it rose, with an accelerating curve. */
    fun exitTransition(): ExitTransition = slideOutVertically(
        animationSpec = exitSpec(),
        targetOffsetY = { fullHeight -> fullHeight },
    ) + fadeOut(exitSpec())

    /** Replaces [enterTransition] while the system has animations off: a short fade, no slide. */
    fun reducedMotionEnterTransition(): EnterTransition = fadeIn(tween(REDUCED_MOTION_FADE_MILLIS))

    /** Replaces [exitTransition] while the system has animations off: a short fade, no slide. */
    fun reducedMotionExitTransition(): ExitTransition = fadeOut(tween(REDUCED_MOTION_FADE_MILLIS))

    /** Timing of the colour and indicator changes of an item when its selection changes. */
    fun <T> stateChangeSpec(): FiniteAnimationSpec<T> = tween(STATE_CHANGE_DURATION_MILLIS, easing = Standard)

    private fun <T> enterSpec(): FiniteAnimationSpec<T> = tween(ENTER_DURATION_MILLIS, easing = EmphasizedDecelerate)

    private fun <T> exitSpec(): FiniteAnimationSpec<T> = tween(EXIT_DURATION_MILLIS, easing = EmphasizedAccelerate)
}

/** [normal] while animations are on, [reduced] while the system has them off. */
internal fun <T> motionAware(
    isReducedMotion: Boolean,
    normal: T,
    reduced: T,
): T = if (isReducedMotion) reduced else normal

@Composable
private fun ItemIcon(
    icon: ImageResource,
    tint: Color,
    badge: StringResource?,
) {
    if (badge == null) {
        Icon(painter = icon.toPainter(), contentDescription = null, tint = tint)
        return
    }
    BadgedBox(
        badge = {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Label.LabelSmall(text = badge)
            }
        },
    ) {
        Icon(painter = icon.toPainter(), contentDescription = null, tint = tint)
    }
}

/**
 * Lays out the bar with the height it will have once [visible] settles: zero as soon as it starts hiding, full as
 * soon as it starts showing. While hiding it is still drawn above its zero-height slot, bottom aligned, so its exit
 * animation plays in place.
 */
private fun Modifier.collapseWhenHidden(
    visible: Boolean,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = collapsedHeight(visible, placeable.height)
    layout(placeable.width, height) {
        placeable.place(x = 0, y = height - placeable.height)
    }
}

/** Height the bar takes in the layout: its measured height while [visible], zero otherwise. */
internal fun collapsedHeight(
    visible: Boolean,
    measuredHeight: Int,
): Int = if (visible) measuredHeight else 0

@DevicePreviews
@Composable
internal fun BottomNavigationBarPreview() {
    PreviewTheme {
        BottomNavigationBar {
            BottomNavigationBarItem(
                label = StringResource.PlainText("Home"),
                icon = Icons.LocationOn,
                selected = true,
                onClick = {},
            )
            BottomNavigationBarItem(
                label = StringResource.PlainText("Explore"),
                icon = Icons.ChevronRight,
                selected = false,
                badge = StringResource.PlainText("New"),
                onClick = {},
            )
            BottomNavigationBarItem(
                label = StringResource.PlainText("Profile"),
                icon = Icons.Close,
                selected = false,
                onClick = {},
            )
        }
    }
}
