package com.lhwdev.selfTestMacro

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.*
import kotlin.math.max
import kotlin.math.roundToInt


/**
 * An internal state used to animate a label and an indicator.
 */
enum class InputPhase {
	// Text field is focused
	Focused,
	
	// Text field is not focused and input text is empty
	UnfocusedEmpty,
	
	// Text field is not focused but input text is not empty
	UnfocusedNotEmpty
}


@Composable
fun TextFieldDecoration(
	inputState: InputPhase,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	label: @Composable (() -> Unit)? = null,
	leadingIcon: @Composable (() -> Unit)? = null,
	trailingIcon: @Composable (() -> Unit)? = null,
	isErrorValue: Boolean = false,
	activeColor: Color = MaterialTheme.colors.primaryActive,
	inactiveColor: Color = LocalContentColor.current,
	errorColor: Color = MaterialTheme.colors.error,
	backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContainerAlpha),
	shape: Shape =
		MaterialTheme.shapes.small.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
	content: @Composable () -> Unit
) {
	@Suppress("UNUSED_ANONYMOUS_PARAMETER")
	TextFieldDecoration(
		inputState = inputState,
		showLabel = label != null,
		activeColor = if(isErrorValue) {
			errorColor
		} else {
			activeColor.copy(alpha = ContentAlpha.high)
		},
		labelInactiveColor = if(isErrorValue) {
			errorColor
		} else {
			inactiveColor.copy(if(enabled) ContentAlpha.medium else ContentAlpha.disabled)
		},
		indicatorInactiveColor = when {
			isErrorValue -> errorColor
			else -> inactiveColor.copy(
				if(enabled) IndicatorInactiveAlpha else ContentAlpha.disabled
			)
		}
	) { labelProgress, labelColor, indicatorWidth, indicatorColor, placeholderOpacity ->
		val leadingColor = inactiveColor.copy(alpha = TrailingLeadingAlpha)
		val trailingColor = if(isErrorValue) errorColor else leadingColor
		
		val decoratedLabel: @Composable (() -> Unit)? =
			if(label != null) {
				@Composable {
					val labelAnimatedStyle = lerp(
						MaterialTheme.typography.subtitle1,
						MaterialTheme.typography.caption,
						labelProgress
					)
					Decoration(
						contentColor = labelColor,
						typography = labelAnimatedStyle,
						content = label
					)
				}
			} else null
		
		// val decoratedPlaceholder: @Composable ((Modifier) -> Unit)? = if(isPlaceholder) {
		// 	@Composable { modifier ->
		// 		Box(modifier.alpha(placeholderOpacity)) {
		// 			Decoration(
		// 				contentColor = inactiveColor,
		// 				typography = MaterialTheme.typography.subtitle1,
		// 				contentAlpha = if(enabled) ContentAlpha.medium else ContentAlpha.disabled,
		// 				content = content
		// 			)
		// 		}
		// 	}
		// } else null
		
		TextFieldLayout(
			modifier = modifier,
			decoratedPlaceholder = null,
			decoratedLabel = decoratedLabel,
			leading = leadingIcon,
			trailing = trailingIcon,
			leadingColor = leadingColor,
			trailingColor = trailingColor,
			labelProgress = labelProgress,
			indicatorWidth = indicatorWidth,
			indicatorColor = indicatorColor,
			backgroundColor = backgroundColor,
			shape = shape
		) {
			content()
		}
	}
}


@Composable
internal fun TextFieldLayout(
	modifier: Modifier,
	decoratedPlaceholder: @Composable ((Modifier) -> Unit)?,
	decoratedLabel: @Composable (() -> Unit)?,
	leading: @Composable (() -> Unit)?,
	trailing: @Composable (() -> Unit)?,
	leadingColor: Color,
	trailingColor: Color,
	labelProgress: Float,
	indicatorWidth: Dp,
	indicatorColor: Color,
	backgroundColor: Color,
	shape: Shape,
	content: @Composable () -> Unit
) {
	Box(
		modifier = Modifier
			.clip(shape)
			.then(modifier) // inevitable; to clip shape; if modifier = Modifier.clickable, ripple goes out of boundary
			.defaultMinSize(
				minWidth = TextFieldMinWidth,
				minHeight = TextFieldMinHeight
			)
			// .background(color = backgroundColor, shape = shape)
			.background(color = backgroundColor)
			.drawIndicatorLine(lineWidth = indicatorWidth, color = indicatorColor)
	) {
		IconsWithTextFieldLayout(
			textField = content,
			label = decoratedLabel,
			placeholder = decoratedPlaceholder,
			leading = leading,
			trailing = trailing,
			singleLine = true,
			leadingColor = leadingColor,
			trailingColor = trailingColor,
			animationProgress = labelProgress
		)
	}
}

/**
 * Layout of the leading and trailing icons and the input field, label and placeholder in
 * [TextField].
 */
@Composable
private fun IconsWithTextFieldLayout(
	textField: @Composable () -> Unit,
	label: @Composable (() -> Unit)?,
	placeholder: @Composable ((Modifier) -> Unit)?,
	leading: @Composable (() -> Unit)?,
	trailing: @Composable (() -> Unit)?,
	singleLine: Boolean,
	leadingColor: Color,
	trailingColor: Color,
	animationProgress: Float
) {
	Layout(
		modifier = Modifier.fillMaxWidth(),
		content = {
			if(leading != null) {
				Box(
					Modifier
						.layoutId("leading")
						.iconPadding(start = HorizontalIconPadding)
				) {
					Decoration(
						contentColor = leadingColor,
						content = leading
					)
				}
			}
			if(trailing != null) {
				Box(
					Modifier
						.layoutId("trailing")
						.iconPadding(end = HorizontalIconPadding)
				) {
					Decoration(
						contentColor = trailingColor,
						content = trailing
					)
				}
			}
			val padding = Modifier.padding(horizontal = TextFieldPadding)
			if(placeholder != null) {
				placeholder(
					Modifier
						.layoutId(PlaceholderId)
						.then(padding)
				)
			}
			if(label != null) {
				Box(
					modifier = Modifier
						.layoutId(LabelId)
						.iconPadding(
							start = TextFieldPadding,
							end = TextFieldPadding
						)
				) { label() }
			}
			Box(
				Modifier
					.layoutId(TextFieldId)
					.then(padding)
			) { textField() }
		}
	) { measurables, incomingConstraints ->
		val topBottomPadding = TextFieldPadding.roundToPx()
		val baseLineOffset = FirstBaselineOffset.roundToPx()
		val bottomPadding = LastBaselineOffset.roundToPx()
		val topPadding = TextFieldTopPadding.roundToPx()
		var occupiedSpaceHorizontally = 0
		
		// measure leading icon
		val constraints = incomingConstraints.copy(minWidth = 0, minHeight = 0)
		val leadingPlaceable =
			measurables.find { it.layoutId == "leading" }?.measure(constraints)
		occupiedSpaceHorizontally += widthOrZero(
			leadingPlaceable
		)
		
		// measure trailing icon
		val trailingPlaceable = measurables.find { it.layoutId == "trailing" }
			?.measure(constraints.offset(horizontal = -occupiedSpaceHorizontally))
		occupiedSpaceHorizontally += widthOrZero(
			trailingPlaceable
		)
		
		// measure label
		val labelConstraints = constraints
			.offset(
				vertical = -bottomPadding,
				horizontal = -occupiedSpaceHorizontally
			)
		val labelPlaceable =
			measurables.find { it.layoutId == LabelId }?.measure(labelConstraints)
		val lastBaseline = labelPlaceable?.get(LastBaseline)?.let {
			if(it != AlignmentLine.Unspecified) it else labelPlaceable.height
		} ?: 0
		val effectiveLabelBaseline = max(lastBaseline, baseLineOffset)
		
		// measure input field
		// input field is laid out differently depending on whether the label is present or not
		val verticalConstraintOffset = if(labelPlaceable != null) {
			-bottomPadding - topPadding - effectiveLabelBaseline
		} else {
			-topBottomPadding * 2
		}
		val textFieldConstraints = incomingConstraints
			.copy(minHeight = 0)
			.offset(
				vertical = verticalConstraintOffset,
				horizontal = -occupiedSpaceHorizontally
			)
		val textFieldPlaceable = measurables
			.first { it.layoutId == TextFieldId }
			.measure(textFieldConstraints)
		
		// measure placeholder
		val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
		val placeholderPlaceable = measurables
			.find { it.layoutId == PlaceholderId }
			?.measure(placeholderConstraints)
		
		val width = calculateWidth(
			leadingPlaceable,
			trailingPlaceable,
			textFieldPlaceable,
			labelPlaceable,
			placeholderPlaceable,
			incomingConstraints
		)
		val height = calculateHeight(
			textFieldPlaceable,
			labelPlaceable,
			effectiveLabelBaseline,
			leadingPlaceable,
			trailingPlaceable,
			placeholderPlaceable,
			incomingConstraints,
			density
		)
		
		layout(width, height) {
			if(widthOrZero(labelPlaceable) != 0) {
				// label's final position is always relative to the baseline
				val labelEndPosition = (baseLineOffset - lastBaseline).coerceAtLeast(0)
				placeWithLabel(
					width,
					height,
					textFieldPlaceable,
					labelPlaceable,
					placeholderPlaceable,
					leadingPlaceable,
					trailingPlaceable,
					singleLine,
					labelEndPosition,
					effectiveLabelBaseline + topPadding,
					animationProgress,
					density
				)
			} else {
				placeWithoutLabel(
					width,
					height,
					textFieldPlaceable,
					placeholderPlaceable,
					leadingPlaceable,
					trailingPlaceable,
					singleLine,
					density
				)
			}
		}
	}
}

private fun calculateWidth(
	leadingPlaceable: Placeable?,
	trailingPlaceable: Placeable?,
	textFieldPlaceable: Placeable,
	labelPlaceable: Placeable?,
	placeholderPlaceable: Placeable?,
	constraints: Constraints
): Int {
	val middleSection = maxOf(
		textFieldPlaceable.width,
		widthOrZero(labelPlaceable),
		widthOrZero(placeholderPlaceable)
	)
	val wrappedWidth =
		widthOrZero(leadingPlaceable) + middleSection + widthOrZero(
			trailingPlaceable
		)
	return max(wrappedWidth, constraints.minWidth)
}

private fun calculateHeight(
	textFieldPlaceable: Placeable,
	labelPlaceable: Placeable?,
	labelBaseline: Int,
	leadingPlaceable: Placeable?,
	trailingPlaceable: Placeable?,
	placeholderPlaceable: Placeable?,
	constraints: Constraints,
	density: Float
): Int {
	val bottomPadding = LastBaselineOffset.value * density
	val topPadding = TextFieldTopPadding.value * density
	val topBottomPadding = TextFieldPadding.value * density
	
	val inputFieldHeight = max(textFieldPlaceable.height, heightOrZero(placeholderPlaceable))
	val middleSectionHeight = if(labelPlaceable != null) {
		labelBaseline + topPadding + inputFieldHeight + bottomPadding
	} else {
		topBottomPadding * 2 + inputFieldHeight
	}
	return maxOf(
		middleSectionHeight.roundToInt(),
		max(heightOrZero(leadingPlaceable), heightOrZero(trailingPlaceable)),
		constraints.minHeight
	)
}

/**
 * Places the provided text field, placeholder and label with respect to the baseline offsets in
 * [TextField] when there is a label. When there is no label, [placeWithoutLabel] is used.
 */
private fun Placeable.PlacementScope.placeWithLabel(
	width: Int,
	height: Int,
	textFieldPlaceable: Placeable,
	labelPlaceable: Placeable?,
	placeholderPlaceable: Placeable?,
	leadingPlaceable: Placeable?,
	trailingPlaceable: Placeable?,
	singleLine: Boolean,
	labelEndPosition: Int,
	textPosition: Int,
	animationProgress: Float,
	density: Float
) {
	val topBottomPadding = (TextFieldPadding.value * density).roundToInt()
	
	leadingPlaceable?.placeRelative(
		0,
		Alignment.CenterVertically.align(leadingPlaceable.height, height)
	)
	trailingPlaceable?.placeRelative(
		width - trailingPlaceable.width,
		Alignment.CenterVertically.align(trailingPlaceable.height, height)
	)
	labelPlaceable?.let {
		// if it's a single line, the label's start position is in the center of the
		// container. When it's a multiline text field, the label's start position is at the
		// top with padding
		val startPosition = if(singleLine) {
			Alignment.CenterVertically.align(it.height, height)
		} else {
			topBottomPadding
		}
		val distance = startPosition - labelEndPosition
		val positionY = startPosition - (distance * animationProgress).roundToInt()
		it.placeRelative(widthOrZero(leadingPlaceable), positionY)
	}
	textFieldPlaceable.placeRelative(widthOrZero(leadingPlaceable), textPosition)
	placeholderPlaceable?.placeRelative(widthOrZero(leadingPlaceable), textPosition)
}

/**
 * Places the provided text field and placeholder in [TextField] when there is no label. When
 * there is a label, [placeWithLabel] is used
 */
private fun Placeable.PlacementScope.placeWithoutLabel(
	width: Int,
	height: Int,
	textPlaceable: Placeable,
	placeholderPlaceable: Placeable?,
	leadingPlaceable: Placeable?,
	trailingPlaceable: Placeable?,
	singleLine: Boolean,
	density: Float
) {
	val topBottomPadding = (TextFieldPadding.value * density).roundToInt()
	
	leadingPlaceable?.placeRelative(
		0,
		Alignment.CenterVertically.align(leadingPlaceable.height, height)
	)
	trailingPlaceable?.placeRelative(
		width - trailingPlaceable.width,
		Alignment.CenterVertically.align(trailingPlaceable.height, height)
	)
	
	// Single line text field without label places its input center vertically. Multiline text
	// field without label places its input at the top with padding
	val textVerticalPosition = if(singleLine) {
		Alignment.CenterVertically.align(textPlaceable.height, height)
	} else {
		topBottomPadding
	}
	textPlaceable.placeRelative(
		widthOrZero(leadingPlaceable),
		textVerticalPosition
	)
	
	// placeholder is placed similar to the text input above
	placeholderPlaceable?.let {
		val placeholderVerticalPosition = if(singleLine) {
			Alignment.CenterVertically.align(placeholderPlaceable.height, height)
		} else {
			topBottomPadding
		}
		it.placeRelative(
			widthOrZero(leadingPlaceable),
			placeholderVerticalPosition
		)
	}
}

/**
 * A draw modifier that draws a bottom indicator line in [TextField]
 */
internal fun Modifier.drawIndicatorLine(lineWidth: Dp, color: Color): Modifier {
	return drawBehind {
		val strokeWidth = lineWidth.value * density
		val y = size.height - strokeWidth / 2
		drawLine(
			color,
			Offset(0f, y),
			Offset(size.width, y),
			strokeWidth
		)
	}
}

private val FirstBaselineOffset = 20.dp
private val LastBaselineOffset = 10.dp
private val TextFieldTopPadding = 4.dp
const val ContainerAlpha = 0.12f


@Composable
fun TextFieldDecoration(
	inputState: InputPhase,
	showLabel: Boolean,
	activeColor: Color,
	labelInactiveColor: Color,
	indicatorInactiveColor: Color,
	content: @Composable (
		labelProgress: Float,
		labelColor: Color,
		indicatorWidth: Dp,
		indicatorColor: Color,
		placeholderOpacity: Float
	) -> Unit
) {
	// Transitions from/to InputPhase.Focused are the most critical in the transition below.
	// UnfocusedEmpty <-> UnfocusedNotEmpty are needed when a single state is used to control
	// multiple text fields.
	val transition = updateTransition(inputState, label = "TextFieldDecoration")
	val labelColor by transition.animateColor(
		transitionSpec = { tween(durationMillis = AnimationDuration) },
		label = "labelColor"
	) {
		when(it) {
			InputPhase.Focused -> activeColor
			InputPhase.UnfocusedEmpty -> labelInactiveColor
			InputPhase.UnfocusedNotEmpty -> labelInactiveColor
		}
	}
	val indicatorColor by transition.animateColor(
		transitionSpec = { tween(durationMillis = AnimationDuration) },
		label = "indicatorColor"
	) {
		when(it) {
			InputPhase.Focused -> activeColor
			InputPhase.UnfocusedEmpty -> indicatorInactiveColor
			InputPhase.UnfocusedNotEmpty -> indicatorInactiveColor
		}
	}
	
	val labelProgress by transition.animateFloat(
		transitionSpec = { tween(durationMillis = AnimationDuration) },
		label = "labelProgress"
	) {
		when(it) {
			InputPhase.Focused -> 1f
			InputPhase.UnfocusedEmpty -> 0f
			InputPhase.UnfocusedNotEmpty -> 1f
		}
	}
	
	val indicatorWidth by transition.animateDp(
		transitionSpec = { tween(durationMillis = AnimationDuration) },
		label = "indicatorWidth"
	) {
		when(it) {
			InputPhase.Focused -> IndicatorFocusedWidth
			InputPhase.UnfocusedEmpty -> IndicatorUnfocusedWidth
			InputPhase.UnfocusedNotEmpty -> IndicatorUnfocusedWidth
		}
	}
	
	val placeholderOpacity by transition.animateFloat(
		transitionSpec = {
			if(InputPhase.Focused isTransitioningTo InputPhase.UnfocusedEmpty) {
				tween(
					durationMillis = PlaceholderAnimationDelayOrDuration,
					easing = LinearEasing
				)
			} else if(InputPhase.UnfocusedEmpty isTransitioningTo InputPhase.Focused ||
				InputPhase.UnfocusedNotEmpty isTransitioningTo InputPhase.UnfocusedEmpty
			) {
				tween(
					durationMillis = PlaceholderAnimationDuration,
					delayMillis = PlaceholderAnimationDelayOrDuration,
					easing = LinearEasing
				)
			} else {
				spring()
			}
		},
		label = "placeholderOpacity"
	) {
		when(it) {
			InputPhase.Focused -> 1f
			InputPhase.UnfocusedEmpty -> if(showLabel) 0f else 1f
			InputPhase.UnfocusedNotEmpty -> 0f
		}
	}
	
	content(
		labelProgress,
		labelColor,
		indicatorWidth,
		indicatorColor,
		placeholderOpacity
	)
}

internal const val TextFieldId = "TextField"
internal const val PlaceholderId = "Hint"
internal const val LabelId = "Label"

private const val AnimationDuration = 150
private const val PlaceholderAnimationDuration = 83
private const val PlaceholderAnimationDelayOrDuration = 67

private val IndicatorUnfocusedWidth = 1.dp
private val IndicatorFocusedWidth = 2.dp
private const val TrailingLeadingAlpha = 0.54f
internal val TextFieldMinHeight = 56.dp
internal val TextFieldMinWidth = 280.dp
internal val TextFieldPadding = 16.dp
internal val HorizontalIconPadding = 12.dp

// Filled text field uses 42% opacity to meet the contrast requirements for accessibility reasons
private const val IndicatorInactiveAlpha = 0.42f


internal fun Modifier.iconPadding(start: Dp = 0.dp, end: Dp = 0.dp) =
	this.then(
		object : LayoutModifier, InspectorValueInfo(
			debugInspectorInfo {
				name = "iconPadding"
				properties["start"] = start
				properties["end"] = end
			}
		) {
			override fun MeasureScope.measure(
				measurable: Measurable,
				constraints: Constraints
			): MeasureResult {
				val horizontal = start.roundToPx() + end.roundToPx()
				val placeable = measurable.measure(constraints.offset(-horizontal))
				val width = if(placeable.nonZero) {
					constraints.constrainWidth(placeable.width + horizontal)
				} else {
					0
				}
				return layout(width, placeable.height) {
					placeable.placeRelative(start.roundToPx(), 0)
				}
			}
		}
	)

private val Placeable.nonZero: Boolean get() = this.width != 0 || this.height != 0
internal fun widthOrZero(placeable: Placeable?) = placeable?.width ?: 0
internal fun heightOrZero(placeable: Placeable?) = placeable?.height ?: 0


/**
 * Set content color, typography and emphasis for [content] composable
 */
@Composable
internal fun Decoration(
	contentColor: Color,
	typography: TextStyle? = null,
	contentAlpha: Float? = null,
	content: @Composable () -> Unit
) {
	val colorAndEmphasis = @Composable {
		CompositionLocalProvider(LocalContentColor provides contentColor) {
			if(contentAlpha != null) {
				CompositionLocalProvider(
					LocalContentAlpha provides contentAlpha,
					content = content
				)
			} else {
				CompositionLocalProvider(
					LocalContentAlpha provides contentColor.alpha,
					content = content
				)
			}
		}
	}
	if(typography != null) ProvideTextStyle(typography, colorAndEmphasis) else colorAndEmphasis()
}
