package com.raxdenstudios.platform.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.raxdenstudios.platform.core.ui.theme.Typography

object Label {

    @Composable
    fun DisplayLargeBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.displayLarge.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun TitleLarge(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.titleLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun TitleLargeBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun TitleMedium(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.titleMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun TitleMediumBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun TitleSmall(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.titleSmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            overflow = overflow
        )
    }

    @Composable
    fun TitleSmallBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            overflow = overflow
        )
    }

    @Composable
    fun HeadlineLarge(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.headlineLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            overflow = overflow
        )
    }

    @Composable
    fun HeadlineMedium(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.headlineMedium,
        color: Color = Color.Unspecified,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            color = color,
            overflow = overflow
        )
    }

    @Composable
    fun HeadlineMediumBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color: Color = Color.Unspecified,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            color = color,
            overflow = overflow
        )
    }

    @Composable
    fun HeadlineSmall(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.headlineSmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            overflow = overflow
        )
    }

    @Composable
    fun HeadlineSmallBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
            overflow = overflow
        )
    }

    @Composable
    fun BodySmall(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.bodySmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun BodyMedium(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.bodyMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun BodyLarge(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.bodyLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun BodyLargeBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun BodyMediumBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun LabelSmall(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.labelSmall,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun LabelLarge(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.labelLarge,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun LabelMedium(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.labelMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun LabelMedium(
        text: Int,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.labelMedium,
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = stringResource(text),
        )
    }

    @Composable
    fun LabelMediumBold(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }

    @Composable
    fun LabelMediumLight(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = Typography.labelMedium.copy(fontWeight = FontWeight.Light),
        textAlign: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        Text(
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            text = text,
        )
    }
}
