package com.lhwdev.selfTestMacro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.icons.ExpandLess
import com.lhwdev.selfTestMacro.icons.ExpandMore
import com.lhwdev.selfTestMacro.icons.Icons
import kotlinx.coroutines.CoroutineScope


@Composable
fun <T> lazyState(init: suspend CoroutineScope.() -> T): State<T?> {
	val state = remember { mutableStateOf<T?>(null) }
	LaunchedEffect(null) {
		state.value = init()
	}
	return state
}


@Composable
fun TextSwitch(
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	text: @Composable () -> Unit,
	switch: @Composable () -> Unit
) {
	Surface(
		modifier = Modifier
			.clickable { onCheckedChange(!checked) }
			.fillMaxWidth()
	) {
		Row {
			Box(Modifier.weight(1f)) { text() }
			switch()
		}
	}
}


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
	Box(modifier) {
		TextFieldDecoration(
			inputState = when {
				expanded -> InputPhase.Focused
				isEmpty -> InputPhase.UnfocusedEmpty
				else -> InputPhase.UnfocusedNotEmpty
			},
			modifier = Modifier.clickable { if(!readonly) setExpanded(true) }.fillMaxWidth(),
			enabled = enabled,
			label = label,
			leadingIcon = leadingIcon,
			trailingIcon = {
				if(!expanded) {
					Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = "Expand")
				} else {
					Icon(imageVector = Icons.Filled.ExpandLess, contentDescription = "Collapse")
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
		
		DropdownMenu(
			expanded = expanded,
			onDismissRequest = { setExpanded(false) },
			modifier = Modifier.fillMaxWidth().sizeIn(maxHeight = DropdownMenuDefaultMaxHeight)
		) {
			dropdown()
		}
	}
}


/**
 * A dropdown menu item, as defined by the Material Design spec.
 *
 * @param onClick Called when the menu item was clicked
 * @param modifier The modifier to be applied to the menu item
 * @param enabled Controls the enabled state of the menu item - when `false`, the menu item
 * will not be clickable and [onClick] will not be invoked
 * @param contentPadding the padding applied to the content of this menu item
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this DropdownMenuItem. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this DropdownMenuItem in different [Interaction]s.
 */
@Composable
fun DropdownMenuItem(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	content: @Composable RowScope.() -> Unit
) {
	DropdownMenuItemContent(
		onClick = onClick,
		modifier = modifier,
		enabled = enabled,
		contentPadding = contentPadding,
		interactionSource = interactionSource,
		content = content
	)
}

@Composable
internal fun DropdownMenuItemContent(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	content: @Composable RowScope.() -> Unit
) {
	Row(
		modifier = modifier
			.clickable(
				enabled = enabled,
				onClick = onClick,
				interactionSource = interactionSource,
				indication = rememberRipple(true)
			)
			.fillMaxWidth()
			// Preferred min and max width used during the intrinsic measurement.
			.sizeIn(
				minWidth = DropdownMenuItemDefaultMinWidth,
				minHeight = DropdownMenuItemDefaultMinHeight
			)
			.padding(contentPadding),
		verticalAlignment = Alignment.CenterVertically
	) {
		val typography = MaterialTheme.typography
		ProvideTextStyle(typography.subtitle1) {
			val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
			CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
				content()
			}
		}
	}
}


private val DropdownMenuItemDefaultMinWidth = 112.dp
private val DropdownMenuItemDefaultMinHeight = 48.dp
private val DropdownMenuDefaultMaxHeight = 370.dp
