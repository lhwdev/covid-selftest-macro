package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit


@Composable
fun AutoSizeText(
	text: String,
	modifier: Modifier = Modifier,
	color: Color = Color.Unspecified,
	fontSize: TextUnit = TextUnit.Unspecified,
	fontStyle: FontStyle? = null,
	fontWeight: FontWeight? = null,
	fontFamily: FontFamily? = null,
	letterSpacing: TextUnit = TextUnit.Unspecified,
	textDecoration: TextDecoration? = null,
	textAlign: TextAlign? = null,
	lineHeight: TextUnit = TextUnit.Unspecified,
	onTextLayout: (TextLayoutResult) -> Unit = {},
	style: TextStyle = LocalTextStyle.current
) {
	BoxWithConstraints {
		var shrunkFontSize = fontSize
		val density = LocalDensity.current
		val resourceLoader = LocalFontLoader.current
		
		val calculateIntrinsics = {
			ParagraphIntrinsics(
				text, TextStyle(
					color = color,
					fontSize = shrunkFontSize,
					fontWeight = fontWeight,
					textAlign = textAlign,
					lineHeight = lineHeight,
					fontFamily = fontFamily,
					textDecoration = textDecoration,
					fontStyle = fontStyle,
					letterSpacing = letterSpacing
				),
				density = density,
				resourceLoader = resourceLoader
			)
		}
		
		var intrinsics = calculateIntrinsics()
		with(LocalDensity.current) {
			val maxWidthPx = maxWidth.toPx()
			while(intrinsics.maxIntrinsicWidth > maxWidthPx) {
				shrunkFontSize *= maxWidthPx / intrinsics.maxIntrinsicWidth
				intrinsics = calculateIntrinsics()
			}
		}
		Text(
			text = text,
			modifier = modifier,
			color = color,
			fontSize = shrunkFontSize,
			fontStyle = fontStyle,
			fontWeight = fontWeight,
			fontFamily = fontFamily,
			letterSpacing = letterSpacing,
			textDecoration = textDecoration,
			textAlign = textAlign,
			lineHeight = lineHeight,
			onTextLayout = onTextLayout,
			style = style
		)
	}
}
