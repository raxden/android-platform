package com.raxdenstudios.platform.ui.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Skeleton loading built from the real components instead of a parallel layout.
 *
 * Wrap a screen in [Skeleton.Container] while its data loads and render the same components with
 * placeholder data: every component that supports it (e.g. [Label]) draws a shimmering block of
 * its own size instead of its content, so the skeleton always matches the loaded screen.
 *
 * Usage:
 * ```
 * Skeleton.Container(visible = uiState.isLoading, contentDescription = loadingText) {
 *     Content(items = if (uiState.isLoading) Item.skeletons else uiState.items)
 * }
 * ```
 *
 * To support it in a component, read [Skeleton.current] and apply [skeleton] (or [skeletonText]
 * for text) to the element that shows content; touches are already blocked by the container:
 * ```
 * Image(modifier = modifier.skeleton(Skeleton.current, shape = Shapes.medium), ...)
 * ```
 */
object Skeleton {

    /** Shimmer of the enclosing [Container], or `null` when no skeleton is shown. */
    val current: Shimmer?
        @Composable
        @ReadOnlyComposable
        get() = LocalSkeletonShimmer.current

    /**
     * Shows [content] as a skeleton while [visible] is `true`: components inside draw shimmering
     * blocks instead of their content, touches are swallowed (no clicks or scroll) and the subtree
     * is hidden from accessibility services, which only read [contentDescription]. Keyboard and
     * D-pad focus are not blocked.
     *
     * The layout and the state of [content] are kept when [visible] changes, so the screen does
     * not jump when loading finishes.
     *
     * @param visible whether [content] is drawn as a skeleton.
     * @param contentDescription localized text read by accessibility services while [visible].
     */
    @Composable
    fun Container(
        visible: Boolean,
        modifier: Modifier = Modifier,
        contentDescription: String? = null,
        content: @Composable () -> Unit,
    ) {
        val shimmer = if (visible) rememberShimmer() else null
        CompositionLocalProvider(LocalSkeletonShimmer provides shimmer) {
            Box(
                modifier = modifier.then(
                    if (visible) Modifier.skeletonContainer(contentDescription) else Modifier,
                ),
            ) {
                content()
            }
        }
    }

    /**
     * Shimmer drawn by skeleton blocks. [progress] is read only while drawing, so the animation
     * redraws the blocks without recomposing the components.
     */
    @Stable
    class Shimmer internal constructor(
        color: Color,
        internal val progress: State<Float>,
    ) {
        internal val colors: List<Color> = listOf(
            color.copy(alpha = BASE_ALPHA),
            color.copy(alpha = HIGHLIGHT_ALPHA),
            color.copy(alpha = BASE_ALPHA),
        )
    }

    /** Shape used for text blocks. */
    internal val TextShape: Shape = RoundedCornerShape(4.dp)

    internal const val TEXT_BAR_HEIGHT_FRACTION = 0.7f
    internal const val LAST_LINE_WIDTH_FRACTION = 0.6f
    internal val TextBarCornerRadius = 4.dp

    /** How far the highlight travels, in element widths, so it fully enters and leaves. */
    internal const val SHIMMER_TRAVEL_FACTOR = 3

    private const val SHIMMER_DURATION_MS = 1_200
    private const val BASE_ALPHA = 0.08f
    private const val HIGHLIGHT_ALPHA = 0.18f
    private const val MAX_TEXT_LINES = 100

    /**
     * Number of text bars that fit in [height] for lines of [lineHeight] pixels: at least 1 and
     * never more than [MAX_TEXT_LINES]; 1 when either size is not positive.
     */
    internal fun textLines(
        height: Float,
        lineHeight: Float,
    ): Int {
        if (lineHeight <= 0f || height <= 0f) return 1
        return (height / lineHeight).roundToInt().coerceIn(1, MAX_TEXT_LINES)
    }

    @Composable
    private fun rememberShimmer(): Shimmer {
        val color = MaterialTheme.colorScheme.onSurface
        val progress = rememberInfiniteTransition(label = "skeleton").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "progress",
        )
        return remember(color, progress) { Shimmer(color = color, progress = progress) }
    }
}

/**
 * Draws a shimmering block in [shape] instead of the content of this element when [shimmer] is
 * not `null` (see [Skeleton]); the element keeps its size, so the layout does not move.
 */
fun Modifier.skeleton(
    shimmer: Skeleton.Shimmer?,
    shape: Shape = Skeleton.TextShape,
): Modifier {
    if (shimmer == null) return this
    return this
        .clip(shape)
        .drawWithContent { drawRect(brush = shimmer.brush(size.width, size.height)) }
}

/**
 * Text variant of [skeleton]: draws one rounded bar per line of [lineHeight] that fits in the
 * element, the last one shorter (anchored to the start edge, also in RTL), so a text reserving
 * several lines reads as text. Draws a single block when [lineHeight] is not in sp. The text
 * alignment is ignored.
 */
fun Modifier.skeletonText(
    shimmer: Skeleton.Shimmer?,
    lineHeight: TextUnit,
): Modifier {
    if (shimmer == null || !lineHeight.isSp) return skeleton(shimmer)
    return drawWithContent {
        val lineHeightPx = lineHeight.toPx()
        val lines = Skeleton.textLines(size.height, lineHeightPx)
        val slotHeight = if (lineHeightPx > 0f) lineHeightPx else size.height
        val barHeight = slotHeight * Skeleton.TEXT_BAR_HEIGHT_FRACTION
        val brush = shimmer.brush(size.width, size.height)
        val radius = CornerRadius(Skeleton.TextBarCornerRadius.toPx())
        clipRect {
            repeat(lines) { line ->
                val isShortLine = lines > 1 && line == lines - 1
                val width = if (isShortLine) {
                    size.width * Skeleton.LAST_LINE_WIDTH_FRACTION
                } else {
                    size.width
                }
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, line * slotHeight + (slotHeight - barHeight) / 2),
                    size = Size(width = width, height = barHeight),
                    cornerRadius = radius,
                )
            }
        }
    }
}

private fun Modifier.skeletonContainer(
    contentDescription: String?,
): Modifier = this
    .clearAndSetSemantics {
        contentDescription?.let { this.contentDescription = it }
    }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                    .changes
                    .forEach { change -> change.consume() }
            }
        }
    }

private fun Skeleton.Shimmer.brush(
    width: Float,
    height: Float,
): Brush {
    val start = -width + progress.value * width * Skeleton.SHIMMER_TRAVEL_FACTOR
    return Brush.linearGradient(
        colors = colors,
        start = Offset(start, 0f),
        end = Offset(start + width, height),
    )
}

private val LocalSkeletonShimmer = compositionLocalOf<Skeleton.Shimmer?> { null }
