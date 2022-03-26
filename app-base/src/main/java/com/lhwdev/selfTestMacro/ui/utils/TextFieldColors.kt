package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


@Composable
fun TextFieldDefaults.myTextFieldColors(
	textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
	backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = BackgroundOpacity),
	cursorColor: Color = MaterialTheme.colors.primary,
	errorCursorColor: Color = MaterialTheme.colors.error,
	focusedIndicatorColor: Color = textColor.copy(alpha = ContentAlpha.high),
	unfocusedIndicatorColor: Color = textColor.copy(alpha = UnfocusedIndicatorLineOpacity),
	errorIndicatorColor: Color = MaterialTheme.colors.error,
	leadingIconColor: Color = textColor.copy(alpha = IconOpacity),
	errorLeadingIconColor: Color = leadingIconColor,
	trailingIconColor: Color = textColor.copy(alpha = IconOpacity),
	errorTrailingIconColor: Color = MaterialTheme.colors.error,
	focusedLabelColor: Color = textColor.copy(alpha = ContentAlpha.high),
	unfocusedLabelColor: Color = textColor.copy(ContentAlpha.medium),
	placeholderColor: Color = textColor.copy(ContentAlpha.medium)
): TextFieldColors = textFieldColors(
	textColor = textColor,
	backgroundColor = backgroundColor,
	cursorColor = cursorColor,
	errorCursorColor = errorCursorColor,
	focusedIndicatorColor = focusedIndicatorColor,
	unfocusedIndicatorColor = unfocusedIndicatorColor,
	errorIndicatorColor = errorIndicatorColor,
	leadingIconColor = leadingIconColor,
	errorLeadingIconColor = errorLeadingIconColor,
	trailingIconColor = trailingIconColor,
	errorTrailingIconColor = errorTrailingIconColor,
	focusedLabelColor = focusedLabelColor,
	unfocusedLabelColor = unfocusedLabelColor,
	placeholderColor = placeholderColor
)
