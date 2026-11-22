package com.raxdenstudios.platform.ui.composable

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.raxdenstudios.commons.android.compose.model.StringResource

/**
 * Typography presets; fontWeight, when provided, overrides the weight in style, and minLines
 * reserves the height of that many lines even when the text is shorter. Inside a visible
 * [Skeleton.Container] each label draws a skeleton bar per line it takes.
 */
object Label {

    @Composable
    fun DisplayLarge(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.displayLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun DisplayMedium(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.displayMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun DisplaySmall(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.displaySmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun TitleLarge(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.titleLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun TitleMedium(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.titleMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun TitleSmall(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.titleSmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        color: Color = Color.Unspecified,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun HeadlineLarge(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.headlineLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        color: Color = Color.Unspecified,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun HeadlineMedium(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.headlineMedium,
        color: Color = Color.Unspecified,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun HeadlineSmall(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.headlineSmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        color: Color = Color.Unspecified,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun BodySmall(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.bodySmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun BodyMedium(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.bodyMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun BodyLarge(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.bodyLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun LabelSmall(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.labelSmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun LabelLarge(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.labelLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    fun LabelMedium(
        text: StringResource,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.labelMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        color: Color = Color.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        fontWeight: FontWeight? = null,
        minLines: Int = 1,
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight, minLines)
    }

    @Composable
    private fun LabelText(
        text: StringResource,
        modifier: Modifier,
        style: TextStyle,
        textAlign: TextAlign,
        maxLines: Int,
        color: Color,
        overflow: TextOverflow,
        fontWeight: FontWeight?,
        minLines: Int,
    ) {
        Text(
            text = text.toText(),
            modifier = modifier.skeletonText(Skeleton.current, style.lineHeight),
            style = style,
            fontWeight = fontWeight,
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            minLines = minLines,
            overflow = overflow,
        )
    }
}
