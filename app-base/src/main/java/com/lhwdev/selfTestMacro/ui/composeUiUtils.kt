package com.lhwdev.selfTestMacro.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.ui.icons.ExpandLess
import com.lhwdev.selfTestMacro.ui.icons.ExpandMore
import com.lhwdev.selfTestMacro.ui.icons.Icons
import com.lhwdev.selfTestMacro.modules.app_base.R
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlin.math.max


enum class VisibilityAnimationState(
	val fromState: Boolean,
	val targetState: Boolean,
	val animating: Boolean
) {
	enter(fromState = false, targetState = true, animating = true),
	visible(fromState = true, targetState = true, animating = false),
	exit(fromState = true, targetState = false, animating = true)
}

@Stable
private class AnimationListEntry<T>(val item: T, state: VisibilityAnimationState) {
	var state by mutableStateOf(state)
	
	override fun toString() = "AnimationListEntry(item=$item, state=$state)"
}


// stack design; does not support diffing
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> AnimateListAsComposable(
	items: List<T>,
	key: (T) -> Any? = { it },
	isOpaque: (T) -> Boolean = { true },
	animation: @Composable (
		item: T,
		state: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) -> Unit,
	content: @Composable (index: Int, T) -> Unit
) {
	val scope = rememberCoroutineScope()
	
	// AnimateListAsComposable assigns list to other value, causing recomposition.
	// So uses workaround, but be aware to call recompositionScope.invalidate()
	var list by remember {
		Ref(items.map { AnimationListEntry(it, VisibilityAnimationState.visible) }.toPersistentList())
	}
	val recomposeScope = currentRecomposeScope
	var lastItems by remember { Ref(items.toList()) }
	
	// diffing goes here
	@Suppress("UnnecessaryVariable")
	val last = list
	
	val result = if(lastItems == items) {
		// 1. fast path
		last
	} else {
		// 2. diff
		// note that this diff is not like Mayer Diff; just checking whether it is as-is
		
		var firstChange = -1
		
		val maxIndex = max(items.size, last.size)
		for(i in 0 until maxIndex) {
			val conflict =
				// list shrunk; index out of new items bound
				i >= items.size ||
					
					// list expanded; index
					i >= last.size ||
					
					// conflict
					items[i] != last[i].item
			
			if(conflict) {
				firstChange = i
				break
			}
		}
		
		if(firstChange == -1) {
			// 3. fast path: no changes
			list
		} else {
			// 4. apply to list
			for(i in firstChange until last.size) {
				val entry = last[i]
				entry.state = VisibilityAnimationState.exit
			}
			
			val new = last.mutate { l ->
				// l.subList(firstConflict, l.lastIndex).clear() // preserved for animation
				l += items.drop(firstChange).map { AnimationListEntry(it, VisibilityAnimationState.enter) }
			}
			
			@Suppress("UNUSED_VALUE")
			list = new
			@Suppress("UNUSED_VALUE")
			lastItems = items.toList()
			new
		}
	}
	
	
	val lastOpaqueIndex = result.indexOfLast {
		val transparent = it.state.animating || !isOpaque(it.item) // inherently transparent like dialog
		!transparent
	}.coerceAtLeast(0)
	
	Box {
		for((index, entry) in result.withIndex()) key(key(entry.item)) {
			Box(Modifier.graphicsLayer {
				alpha = if(index >= lastOpaqueIndex) 1f else 0f
			}) {
				animation(
					entry.item,
					entry.state,
					{
						val newIndex = list.indexOf(entry)
						if(newIndex == -1) return@animation
						when(entry.state) {
							VisibilityAnimationState.enter ->
								entry.state = VisibilityAnimationState.visible
							VisibilityAnimationState.visible -> Unit // no-op
							VisibilityAnimationState.exit -> {
								list = list.removeAt(newIndex)
								recomposeScope.invalidate()
							}
						}
					}
				) { content(index, entry.item) }
			}
		}
	}
}


@Composable
fun AnimateHeight(
	visible: Boolean,
	modifier: Modifier = Modifier,
	animationSpec: AnimationSpec<Float> = spring(),
	content: @Composable () -> Unit
) {
	val scope = rememberCoroutineScope()
	val height = remember { Animatable(-1f) }
	
	Layout(
		content = content,
		modifier = modifier.clipToBounds()
	) { measurables, constraints ->
		val measurable = measurables.single()
		val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
		
		val heightValue = height.value.toInt()
		val targetInt = if(visible) placeable.height else 0
		val target = targetInt.toFloat()
		if(heightValue == -1) scope.launch { height.snapTo(target) }
		if(target != height.targetValue) scope.launch {
			height.animateTo(target, animationSpec)
		}
		
		layout(placeable.width, if(heightValue == -1) targetInt else heightValue) {
			placeable.place(0, 0)
		}
	}
}


@Composable
fun SmallIconButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	content: @Composable () -> Unit
) {
	Box(
		modifier = modifier
			.clickable(
				onClick = onClick,
				enabled = enabled,
				role = Role.Button,
				interactionSource = interactionSource,
				indication = rememberRipple(bounded = false, radius = 18.dp)
			)
			.then(Modifier.size(36.dp)),
		contentAlignment = Alignment.Center
	) {
		val contentAlpha = if(enabled) LocalContentAlpha.current else ContentAlpha.disabled
		CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
	}
}


@Composable
fun RoundButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	border: BorderStroke? = null,
	colors: ButtonColors = ButtonDefaults.textButtonColors(contentColor = DefaultContentColor),
	contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
	elevation: ButtonElevation? = null,
	icon: @Composable (() -> Unit)? = null,
	trailingIcon: @Composable (() -> Unit)? = null,
	text: @Composable RowScope.() -> Unit
) {
	val shape = RoundedCornerShape(percent = 100)
	Button(
		onClick = onClick,
		modifier = modifier,
		enabled = enabled,
		interactionSource = interactionSource,
		elevation = elevation,
		shape = shape,
		border = border,
		colors = colors,
		contentPadding = contentPadding
	) {
		if(icon == null && trailingIcon == null) Spacer(Modifier.width(4.dp))
		if(icon != null) {
			Box(Modifier.size(18.dp)) { icon() }
			Spacer(Modifier.width(8.dp))
		}
		text()
		if(icon == null && trailingIcon == null) Spacer(Modifier.width(4.dp))
		if(trailingIcon != null) {
			Spacer(Modifier.width(8.dp))
			Box(Modifier.size(18.dp)) { trailingIcon() }
		}
	}
}

@Composable
fun IconOnlyTopAppBar(
	navigationIcon: Painter,
	contentDescription: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	TopAppBar(
		title = {},
		navigationIcon = {
			IconButton(onClick = onClick) {
				Icon(painter = navigationIcon, contentDescription = contentDescription)
			}
		},
		elevation = 0.dp,
		backgroundColor = Color.Transparent
	)
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
