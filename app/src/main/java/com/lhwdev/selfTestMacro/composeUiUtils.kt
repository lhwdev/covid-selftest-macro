package com.lhwdev.selfTestMacro

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.icons.ExpandLess
import com.lhwdev.selfTestMacro.icons.ExpandMore
import com.lhwdev.selfTestMacro.icons.Icons
import kotlinx.coroutines.launch
import kotlin.math.max


// do not supports inserting middle of the list
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> AnimateListAsComposable(
	list: List<T>,
	key: (T) -> Any? = { it },
	content: @Composable (index: Int, T) -> Unit
) {
	val scope = rememberCoroutineScope()
	val backing = remember { mutableStateListOf<T>().also { it += list } }
	val animatables = remember {
		mutableStateListOf<Animatable<Float, AnimationVector1D>>().also {
			it += List(list.size) { Animatable(1f) }
		}
	}
	
	fun removeAt(index: Int) { // is this safe if another item is removed during animation?
		scope.launch {
			animatables[index].animateTo(0f)
			backing.removeAt(index)
			animatables.removeAt(index)
		}
	}
	
	fun add(index: Int, value: T) {
		val animatable = Animatable(0f)
		backing.add(index, value)
		animatables.add(index, animatable)
		scope.launch { animatable.animateTo(1f) }
	}
	
	if(list != backing) for(i in 0 until max(list.size, backing.size)) {
		when {
			// list shrunk
			i >= list.size -> removeAt(i)
			
			// list expanded
			i >= backing.size -> add(i, list[i])
			
			// conflict, we do not track items
			list[i] != backing[i] -> {
				removeAt(i)
				add(i, list[i])
			}
		}
	}
	
	
	for((index, item) in backing.withIndex()) key(key(item)) {
		val animatable = animatables[index]
		val modifier = Modifier.graphicsLayer { alpha = animatable.value }
		Box(modifier) { content(index, item) }
	}
}


@Composable
fun TextIconButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	border: BorderStroke? = null,
	colors: ButtonColors = ButtonDefaults.textButtonColors(contentColor = DefaultContentColor),
	icon: @Composable (() -> Unit)? = null,
	trailingIcon: @Composable (() -> Unit)? = null,
	text: @Composable RowScope.() -> Unit
) {
	val contentColor by colors.contentColor(enabled)
	val shape = RoundedCornerShape(percent = 100)
	
	var modifier2 = modifier
		.clip(shape)
		.clickable(
			onClick = onClick,
			enabled = enabled,
			role = Role.Button,
			interactionSource = interactionSource,
			indication = rememberRipple(bounded = false)
		)
		.then(IconButtonSizeModifier)
		.padding(12.dp)
		.sizeIn(minWidth = ButtonDefaults.MinWidth, minHeight = ButtonDefaults.MinHeight)
	if(border != null) modifier2 = modifier2.border(border, shape = shape)
	
	Row(
		modifier = modifier2,
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically
	) {
		val contentAlpha = if(enabled) LocalContentAlpha.current else ContentAlpha.disabled
		CompositionLocalProvider(
			LocalContentAlpha provides contentAlpha,
			LocalContentColor provides contentColor.copy(alpha = 1f)
		) {
			ProvideTextStyle(MaterialTheme.typography.button) {
				/*if(icon == null) */Spacer(Modifier.width(4.dp))
				if(icon != null) {
					Box(Modifier.size(18.dp)) { icon() }
					Spacer(Modifier.width(8.dp))
				}
				text()
				/*if(trailingIcon == null) */Spacer(Modifier.width(4.dp))
				if(trailingIcon != null) {
					Spacer(Modifier.width(8.dp))
					Box(Modifier.size(18.dp)) { trailingIcon() }
				}
			}
		}
	}
}


// per-component definitions of this size.
// Diameter of the IconButton, to allow for correct minimum touch target size for accessibility
private val IconButtonSizeModifier = Modifier.height(48.dp)


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
			modifier = Modifier
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
				modifier = Modifier.width(maxWidth).sizeIn(maxHeight = DropdownMenuDefaultMaxHeight)
			) {
				dropdown()
			}
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
			val contentAlpha = if(enabled) ContentAlpha.high else ContentAlpha.disabled
			CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
				content()
			}
		}
	}
}


private val DropdownMenuItemDefaultMinWidth = 112.dp
private val DropdownMenuItemDefaultMinHeight = 48.dp
private val DropdownMenuDefaultMaxHeight = 370.dp
