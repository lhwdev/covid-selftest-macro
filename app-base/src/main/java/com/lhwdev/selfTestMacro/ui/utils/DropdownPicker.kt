package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.modules.app_base.R
import com.lhwdev.selfTestMacro.ui.ContainerAlpha
import com.lhwdev.selfTestMacro.ui.InputPhase
import com.lhwdev.selfTestMacro.ui.TextFieldDecoration
import com.lhwdev.selfTestMacro.ui.icons.ExpandLess
import com.lhwdev.selfTestMacro.ui.icons.ExpandMore
import com.lhwdev.selfTestMacro.ui.icons.Icons
import com.lhwdev.selfTestMacro.ui.primaryActive


// note: added in latest Compose; if released, will discard this and replace with it
@Composable
fun DropdownPicker(
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	readonly: Boolean = false,
	label: @Composable (() -> Unit)? = null,
	leadingIcon: @Composable (() -> Unit)? = null,
	isErrorValue: Boolean = false,
	isEmpty: Boolean = false,
	activeColor: Color = MaterialTheme.colors.primaryActive,
	inactiveColor: Color = LocalContentColor.current,
	errorColor: Color = MaterialTheme.colors.error,
	backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContainerAlpha),
	shape: Shape =
		MaterialTheme.shapes.small.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
	dropdown: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit,
	content: @Composable () -> Unit
) {
	val (expanded, setExpanded) = remember { mutableStateOf(false) }
	
	DropdownPicker(
		expanded = expanded,
		setExpanded = setExpanded,
		enabled = enabled,
		readonly = readonly,
		label = label,
		leadingIcon = leadingIcon,
		isErrorValue = isErrorValue,
		isEmpty = isEmpty,
		activeColor = activeColor,
		inactiveColor = inactiveColor,
		errorColor = errorColor,
		backgroundColor = backgroundColor,
		shape = shape,
		dropdown = { dropdown { setExpanded(false) } },
		modifier = modifier,
		content = content
	)
}

@Composable
fun DropdownPicker(
	expanded: Boolean,
	setExpanded: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	readonly: Boolean = false,
	label: @Composable (() -> Unit)? = null,
	leadingIcon: @Composable (() -> Unit)? = null,
	isErrorValue: Boolean = false,
	isEmpty: Boolean = false,
	activeColor: Color = MaterialTheme.colors.primaryActive,
	inactiveColor: Color = LocalContentColor.current,
	errorColor: Color = MaterialTheme.colors.error,
	backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContainerAlpha),
	shape: Shape =
		MaterialTheme.shapes.small.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
	dropdown: @Composable ColumnScope.() -> Unit,
	content: @Composable () -> Unit
) {
	Column(modifier) {
		TextFieldDecoration(
			inputState = when {
				expanded -> InputPhase.Focused
				isEmpty -> InputPhase.UnfocusedEmpty
				else -> InputPhase.UnfocusedNotEmpty
			},
			innerModifier = Modifier
				.clickable { if(!readonly) setExpanded(true) }
				.fillMaxWidth(),
			enabled = enabled,
			label = label,
			leadingIcon = leadingIcon,
			trailingIcon = {
				if(!expanded) {
					Icon(
						imageVector = Icons.Filled.ExpandMore,
						contentDescription = stringResource(R.string.action_expand_more)
					)
				} else {
					Icon(
						imageVector = Icons.Filled.ExpandLess,
						contentDescription = stringResource(R.string.action_expand_less)
					)
				}
			},
			isErrorValue = isErrorValue,
			activeColor = activeColor,
			inactiveColor = inactiveColor,
			errorColor = errorColor,
			backgroundColor = backgroundColor,
			shape = shape,
			content = content
		)
		
		BoxWithConstraints {
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { setExpanded(false) },
				modifier = Modifier
					.width(maxWidth)
					.sizeIn(maxHeight = DropdownMenuDefaultMaxHeight)
			) {
				dropdown()
			}
		}
	}
}

private val DropdownMenuDefaultMaxHeight = 370.dp
