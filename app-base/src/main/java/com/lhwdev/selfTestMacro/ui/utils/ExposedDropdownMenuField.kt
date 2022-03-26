package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape


@Composable
fun ExposedDropdownMenuField(
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	isEmpty: Boolean,
	fieldModifier: Modifier = Modifier,
	dropdownContentModifier: Modifier = Modifier,
	enabled: Boolean = true,
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
		modifier = fieldModifier
	) {
		ClickableTextFieldDecoration(
			isEmpty = isEmpty,
			onClick = { onExpandedChange(!expanded) },
			enabled = enabled,
			label = label,
			leadingIcon = leadingIcon,
			trailingIcon = {
				ExposedDropdownMenuDefaults.TrailingIcon(expanded)
			},
			isError = false,
			shape = shape,
			colors = colors,
			content = fieldContent
		)
		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { onExpandedChange(false) },
			modifier = dropdownContentModifier,
			content = dropdownContent
		)
	}
}
