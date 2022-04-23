@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.lhwdev.selfTestMacro.ui.isLight
import kotlinx.coroutines.delay


@Composable
fun SimpleTooltip(
	tooltipText: String,
	modifier: Modifier = Modifier,
	tooltipModifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	val (expanded, setExpanded) = remember { mutableStateOf(false) }
	Tooltip(
		expanded = expanded,
		setExpanded = setExpanded,
		modifier = tooltipModifier
	) { Text(tooltipText) }
	
	Box(modifier.pointerInput(Unit) {
		detectTapGestures(
			onLongPress = { setExpanded(true) }
		)
	}) {
		content()
	}
}


/**
 * Tooltip implementation for AndroidX Jetpack Compose.
 * Based on material [DropdownMenu] implementation
 *
 * A [Tooltip] behaves similarly to a [Popup], and will use the position of the parent layout
 * to position itself on screen. Commonly a [Tooltip] will be placed in a [Box] with a sibling
 * that will be used as the 'anchor'. Note that a [Tooltip] by itself will not take up any
 * space in a layout, as the tooltip is displayed in a separate window, on top of other content.
 *
 * The [content] of a [Tooltip] will typically be [Text], as well as custom content.
 *
 * [Tooltip] changes its positioning depending on the available space, always trying to be
 * fully visible. It will try to expand horizontally, depending on layout direction, to the end of
 * its parent, then to the start of its parent, and then screen end-aligned. Vertically, it will
 * try to expand to the bottom of its parent, then from the top of its parent, and then screen
 * top-aligned. An [offset] can be provided to adjust the positioning of the menu for cases when
 * the layout bounds of its parent do not coincide with its visual bounds. Note the offset will
 * be applied in the direction in which the menu will decide to expand.
 *
 * @param expanded Whether the tooltip is currently visible to the user
 *
 * @see androidx.compose.material.DropdownMenu
 * @see androidx.compose.material.DropdownMenuPositionProvider
 * @see androidx.compose.ui.window.Popup
 *
 * @author Artyom Krivolapov
 */
@Composable
fun Tooltip(
	expanded: Boolean,
	setExpanded: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	timeoutMillis: Long = TooltipTimeout,
	backgroundColor: Color = Color.Black,
	offset: DpOffset = DpOffset(0.dp, 0.dp),
	properties: PopupProperties = PopupProperties(focusable = true),
	content: @Composable ColumnScope.() -> Unit,
) {
	val expandedStates = remember { MutableTransitionState(false) }
	expandedStates.targetState = expanded
	
	if(expandedStates.currentState || expandedStates.targetState) {
		if(expandedStates.isIdle) {
			LaunchedEffect(timeoutMillis, expanded) {
				delay(timeoutMillis)
				setExpanded(false)
			}
		}
		
		Popup(
			onDismissRequest = { setExpanded(false) },
			popupPositionProvider = DropdownMenuPositionProvider(offset, LocalDensity.current),
			properties = properties,
		) {
			Box(
				// Add space for elevation shadow
				modifier = Modifier.padding(TooltipElevation)
			) {
				TooltipContent(expandedStates, backgroundColor, modifier, content)
			}
		}
	}
}


/** @see androidx.compose.material.DropdownMenuContent */
@Composable
private fun TooltipContent(
	expandedStates: MutableTransitionState<Boolean>,
	backgroundColor: Color,
	modifier: Modifier,
	content: @Composable ColumnScope.() -> Unit,
) {
	// Tooltip open/close animation.
	val transition = updateTransition(expandedStates, "Tooltip")
	
	val alpha by transition.animateFloat(
		label = "alpha",
		transitionSpec = {
			if(false isTransitioningTo true) {
				// Dismissed to expanded
				tween(durationMillis = InTransitionDuration)
			} else {
				// Expanded to dismissed.
				tween(durationMillis = OutTransitionDuration)
			}
		}
	) { if(it) 1f else 0f }
	
	Surface(
		color = backgroundColor.copy(alpha = 0.75f),
		contentColor = MaterialTheme.colors.contentColorFor(backgroundColor)
			.takeOrElse { backgroundColor.onColor() },
		shape = MaterialTheme.shapes.medium,
		modifier = Modifier.alpha(alpha),
		elevation = TooltipElevation
	) {
		val p = TooltipPadding
		Column(
			modifier = modifier
				.padding(start = p, top = p * 0.5f, end = p, bottom = p * 0.7f)
				.width(IntrinsicSize.Max),
			content = content
		)
	}
}

private val TooltipElevation = 16.dp
private val TooltipPadding = 16.dp

// Tooltip open/close animation duration.
private const val InTransitionDuration = 64
private const val OutTransitionDuration = 240

// Default timeout before tooltip close
private const val TooltipTimeout = 2_000L - OutTransitionDuration


// Color utils

/**
 * Calculates an 'on' color for this color.
 *
 * @return [Color.Black] or [Color.White], depending on [isLight].
 */
private fun Color.onColor(): Color = if(isLight) Color.Black else Color.White
