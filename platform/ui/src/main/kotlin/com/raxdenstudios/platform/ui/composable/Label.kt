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
import com.raxdenstudios.platform.ui.model.StringResource

/** Typography presets; fontWeight, when provided, overrides the weight in style. */
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        LabelText(text, modifier, style, textAlign, maxLines, color, overflow, fontWeight)
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
    ) {
        Text(
            text = text.toText(),
            modifier = modifier,
            style = style,
            fontWeight = fontWeight,
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}
