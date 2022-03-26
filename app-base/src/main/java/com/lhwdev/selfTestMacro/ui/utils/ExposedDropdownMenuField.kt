package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun ExposedDropdownMenuField(
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	isEmpty: Boolean,
	modifier: Modifier = Modifier,
	dropdownContentModifier: Modifier = Modifier,
	enabled: Boolean = true,
	readOnly: Boolean = false,
	label: @Composable (() -> Unit)? = null,
	leadingIcon: @Composable (() -> Unit)? = null,
	isError: Boolean = false,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	shape: Shape =
		MaterialTheme.shapes.small.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
	colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
	fieldContent: @Composable () -> Unit,
	dropdownContent: @Composable ColumnScope.() -> Unit
) {
	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = onExpandedChange,
		modifier = modifier
	) {
		Box(
			modifier = Modifier
				.background(colors.backgroundColor(enabled).value, shape)
				.indicatorLine(enabled, isError, interactionSource, colors)
				.defaultMinSize(
					minWidth = TextFieldDefaults.MinWidth,
					minHeight = TextFieldDefaults.MinHeight
				)
				.clickable(interactionSource = interactionSource, indication = LocalIndication.current) {
					onExpandedChange(!expanded)
				}
				.clip(shape)
		) {
			TextFieldDefaults.TextFieldDecorationBox(
				// [value] is only used to determine if the text is empty. See TextFieldImpl.kt.
				value = if(isEmpty) "" else "핳핳핳",
				innerTextField = {
					Box(modifier = Modifier.fillMaxWidth()) { fieldContent() }
				},
				enabled = enabled,
				singleLine = true,
				visualTransformation = VisualTransformation.None,
				interactionSource = interactionSource,
				isError = isError,
				label = label,
				placeholder = null,
				leadingIcon = leadingIcon,
				trailingIcon = {
					ExposedDropdownMenuDefaults.TrailingIcon(expanded)
				},
				colors = colors
			)
			
			ExposedDropdownMenu(
				expanded = expanded,
				onDismissRequest = { onExpandedChange(false) },
				modifier = dropdownContentModifier,
				content = dropdownContent
			)
		}
	}
}
