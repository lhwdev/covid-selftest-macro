package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun ClickableTextFieldDecoration(
	isEmpty: Boolean,
	isFocused: Boolean = false,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	label: @Composable (() -> Unit)? = null,
	placeholder: @Composable (() -> Unit)? = null,
	leadingIcon: @Composable (() -> Unit)? = null,
	trailingIcon: @Composable (() -> Unit)? = null,
	isError: Boolean = false,
	// singleLine: Boolean = false,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	shape: Shape = TextFieldDefaults.TextFieldShape,
	colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
	content: @Composable () -> Unit
) {
	Box(
		modifier = modifier
			.defaultMinSize(
				minWidth = TextFieldDefaults.MinWidth,
				minHeight = TextFieldDefaults.MinHeight
			)
			.clip(shape)
			.background(colors.backgroundColor(enabled).value)
			.indicatorLine(enabled, isError, interactionSource, colors)
			.focusProperties { canFocus = isFocused }
			.focusable(enabled, interactionSource)
			.clickable(
				interactionSource = interactionSource,
				indication = LocalIndication.current,
				enabled = enabled,
				onClick = onClick
			),
		contentAlignment = Alignment.CenterStart
	) {
		TextFieldDefaults.TextFieldDecorationBox(
			// [value] is only used to determine if the text is empty. See TextFieldImpl.kt.
			value = if(isEmpty) "" else "핳핳핳",
			innerTextField = {
				Box(modifier = Modifier.fillMaxWidth()) { content() }
			},
			enabled = enabled,
			singleLine = true,
			visualTransformation = VisualTransformation.None,
			interactionSource = interactionSource, // for focus, nothing like indication
			isError = isError,
			label = label,
			placeholder = placeholder,
			leadingIcon = leadingIcon,
			trailingIcon = trailingIcon,
			colors = colors
		)
	}
}