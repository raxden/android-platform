package com.raxdenstudios.platform.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SHINE_DELAY_MILLIS = 350L
private const val SHINE_DURATION_MILLIS = 900
private const val SHINE_ALPHA = 0.35f
private const val SHINE_WIDTH_FRACTION = 0.35f
private val ShineColors = listOf(Color.Transparent, Color.White.copy(alpha = SHINE_ALPHA), Color.Transparent)

/**
 * A soft light sweeping once across the element, clipped to [shape], shortly after it appears. Skipped while the
 * system has animations off (see [isReducedMotion]).
 *
 * It plays once per attach of the modifier node, not once per app or screen: each time the element enters
 * composition again it sweeps again, e.g. a lazy list item scrolled back into view or a destination navigated back
 * to.
 */
fun Modifier.appearShine(
    shape: Shape,
): Modifier = this then AppearShineElement(shape)

/**
 * Horizontal position of the leading edge of the shine band for a [progress] between 0 and 1: it starts one band
 * before the element and ends one band after it.
 */
internal fun shineBandStart(
    width: Float,
    bandWidth: Float,
    progress: Float,
): Float = -bandWidth + (width + bandWidth * 2) * progress

private data class AppearShineElement(
    val shape: Shape,
) : ModifierNodeElement<AppearShineNode>() {

    override fun create(): AppearShineNode = AppearShineNode(shape)

    override fun update(
        node: AppearShineNode,
    ) = node.update(shape)

    override fun InspectorInfo.inspectableProperties() {
        name = "appearShine"
        properties["shape"] = shape
    }
}

/** Sweeps the shine once after attaching; the outline path is rebuilt only when the size or shape changes. */
private class AppearShineNode(
    private var shape: Shape,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    private val progress = Animatable(0f)
    private val path = Path()
    private var pathSize = Size.Unspecified
    private var pathLayoutDirection: LayoutDirection? = null

    override fun onAttach() {
        if (progress.value > 0f || isReducedMotion(currentValueOf(LocalContext))) return
        coroutineScope.launch {
            delay(SHINE_DELAY_MILLIS)
            progress.animateTo(1f, tween(SHINE_DURATION_MILLIS, easing = FastOutSlowInEasing))
        }
    }

    fun update(
        shape: Shape,
    ) {
        if (this.shape == shape) return
        this.shape = shape
        pathSize = Size.Unspecified
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val value = progress.value
        if (value <= 0f || value >= 1f) return
        updatePath()
        val bandWidth = size.width * SHINE_WIDTH_FRACTION
        val start = shineBandStart(size.width, bandWidth, value)
        clipPath(path) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = ShineColors,
                    start = Offset(start - bandWidth, 0f),
                    end = Offset(start, size.height),
                ),
            )
        }
    }

    private fun ContentDrawScope.updatePath() {
        if (pathSize == size && pathLayoutDirection == layoutDirection) return
        path.reset()
        path.addOutline(shape.createOutline(size, layoutDirection, this))
        pathSize = size
        pathLayoutDirection = layoutDirection
    }
}
