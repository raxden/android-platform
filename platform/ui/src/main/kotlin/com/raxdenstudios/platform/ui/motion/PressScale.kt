package com.raxdenstudios.platform.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import com.raxdenstudios.platform.ui.haptic.AppHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PRESS_DAMPING_RATIO = 0.6f
private const val REST_SCALE = 1f

/** Quick and slightly bouncy spring used by [pressScale] unless another spec is given. */
private val DefaultPressSpring: AnimationSpec<Float> = spring(
    dampingRatio = PRESS_DAMPING_RATIO,
    stiffness = Spring.StiffnessMedium,
)

/**
 * Scales the element down to [pressedScale] with [animationSpec] while [interactionSource] is pressed, and back up
 * when released. Pass the same interaction source the clickable uses. With [hapticOnPress] a light tap is felt as
 * the press starts. Nothing scales while the system has animations off (see [isReducedMotion]).
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float,
    hapticOnPress: Boolean = false,
    animationSpec: AnimationSpec<Float> = DefaultPressSpring,
): Modifier = this then PressScaleElement(interactionSource, pressedScale, hapticOnPress, animationSpec)

/** The scale [pressScale] animates towards for the given press state. */
internal fun pressScaleTarget(
    isPressed: Boolean,
    isReducedMotion: Boolean,
    pressedScale: Float,
): Float = if (isPressed && isReducedMotion.not()) pressedScale else REST_SCALE

/** Adds or removes the press an [interaction] starts or ends; other interactions leave [presses] untouched. */
internal fun trackPress(
    presses: MutableList<PressInteraction.Press>,
    interaction: Interaction,
) {
    when (interaction) {
        is PressInteraction.Press -> presses += interaction
        is PressInteraction.Release -> presses -= interaction.press
        is PressInteraction.Cancel -> presses -= interaction.press
    }
}

private data class PressScaleElement(
    val interactionSource: InteractionSource,
    val pressedScale: Float,
    val hapticOnPress: Boolean,
    val animationSpec: AnimationSpec<Float>,
) : ModifierNodeElement<PressScaleNode>() {

    override fun create(): PressScaleNode = PressScaleNode(
        interactionSource = interactionSource,
        pressedScale = pressedScale,
        hapticOnPress = hapticOnPress,
        animationSpec = animationSpec,
    )

    override fun update(
        node: PressScaleNode,
    ) = node.update(interactionSource, pressedScale, hapticOnPress, animationSpec)

    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["pressedScale"] = pressedScale
        properties["hapticOnPress"] = hapticOnPress
    }
}

/** Collects the presses of its interaction source and animates the layer scale towards the pressed or rest value. */
private class PressScaleNode(
    private var interactionSource: InteractionSource,
    private var pressedScale: Float,
    private var hapticOnPress: Boolean,
    private var animationSpec: AnimationSpec<Float>,
) : Modifier.Node(),
    LayoutModifierNode,
    CompositionLocalConsumerModifierNode {

    private val scale = Animatable(REST_SCALE)
    private var collectJob: Job? = null

    override fun onAttach() {
        collectPresses()
    }

    fun update(
        interactionSource: InteractionSource,
        pressedScale: Float,
        hapticOnPress: Boolean,
        animationSpec: AnimationSpec<Float>,
    ) {
        this.pressedScale = pressedScale
        this.hapticOnPress = hapticOnPress
        this.animationSpec = animationSpec
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            if (isAttached) collectPresses()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }

    /**
     * Starts collecting the presses of the current interaction source. Presses of a previous source are forgotten,
     * so a scale left pressed by it springs back to rest.
     */
    private fun collectPresses() {
        collectJob?.cancel()
        resetToRest()
        collectJob = coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                val wasPressed = presses.isNotEmpty()
                trackPress(presses, interaction)
                val isPressed = presses.isNotEmpty()
                if (isPressed != wasPressed) onPressChanged(isPressed)
            }
        }
    }

    private fun resetToRest() {
        if (scale.targetValue == REST_SCALE) return
        coroutineScope.launch { scale.animateTo(REST_SCALE, animationSpec) }
    }

    private fun onPressChanged(
        isPressed: Boolean,
    ) {
        if (isPressed && hapticOnPress) AppHaptics(currentValueOf(LocalView)).tap()
        val target = pressScaleTarget(
            isPressed = isPressed,
            isReducedMotion = isReducedMotion(currentValueOf(LocalContext)),
            pressedScale = pressedScale,
        )
        coroutineScope.launch { scale.animateTo(target, animationSpec) }
    }
}
