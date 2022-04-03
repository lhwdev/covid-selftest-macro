package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.ui.systemUi.rememberIsVisible
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


// https://gist.github.com/vganin/a9a84653a9f48a2d669910fbd48e32d5
@Composable
fun NumberPicker(
	value: Int,
	setValue: (Int) -> Unit,
	modifier: Modifier = Modifier,
	range: IntRange? = null,
	maxIndex: Int = 1,
	content: @Composable (Int) -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	val numbersColumnHeight = 100.dp
	val halvedNumbersColumnHeight = numbersColumnHeight / 2
	val numbersColumnHeightPx = with(LocalDensity.current) { numbersColumnHeight.toPx() }
	val halvedNumbersColumnHeightPx = numbersColumnHeightPx / 2f
	
	val (input, setInput) = remember { mutableStateOf<TextFieldValue?>(null) }
	
	// do not change while editing for ux
	val savedState = remember { mutableStateOf(value) }
	val state = if(input != null) savedState.value else value
	
	val animatedOffset = remember { Animatable(0f) }.apply {
		if(range != null) {
			val offsetRange = remember(state, range) {
				val first = -(range.last - state) * halvedNumbersColumnHeightPx
				val last = -(range.first - state) * halvedNumbersColumnHeightPx
				first..last
			}
			updateBounds(offsetRange.start, offsetRange.endInclusive)
		}
	}
	
	fun animatedStateValue(offset: Float): Int = state - (offset / halvedNumbersColumnHeightPx).toInt()
	
	val coercedAnimatedOffset by derivedStateOf { animatedOffset.value % halvedNumbersColumnHeightPx }
	val animatedStateValue by derivedStateOf { animatedStateValue(animatedOffset.value) }
	
	Column(
		modifier = modifier
			.height(halvedNumbersColumnHeight * (2 * maxIndex + 1))
			.clipToBounds()
			.draggable(
				enabled = input == null,
				orientation = Orientation.Vertical,
				state = rememberDraggableState { deltaY ->
					coroutineScope.launch {
						animatedOffset.snapTo(animatedOffset.value + deltaY)
					}
				},
				onDragStopped = { velocity ->
					coroutineScope.launch {
						val endValue = animatedOffset.fling(
							initialVelocity = velocity,
							animationSpec = exponentialDecay(frictionMultiplier = 10f),
							adjustTarget = { target ->
								val coercedTarget = target % halvedNumbersColumnHeightPx
								val coercedAnchors =
									listOf(-halvedNumbersColumnHeightPx, 0f, halvedNumbersColumnHeightPx)
								val coercedPoint = coercedAnchors.minByOrNull { abs(it - coercedTarget) }!!
								val base = halvedNumbersColumnHeightPx * (target / halvedNumbersColumnHeightPx).toInt()
								coercedPoint + base
							}
						).endState.value
						
						setValue(animatedStateValue(endValue).let { if(range == null) it else it.coerceIn(range) })
						animatedOffset.snapTo(0f)
					}
				}
			)
	) {
		Box(
			modifier = Modifier
				.align(Alignment.CenterHorizontally)
				.offset { IntOffset(x = 0, y = (coercedAnimatedOffset + halvedNumbersColumnHeightPx).roundToInt()) }
		) {
			val baseLabelModifier = Modifier.align(Alignment.Center)
				.height(halvedNumbersColumnHeight)
			
			@Composable
			fun Item(index: Int) {
				val distanceProportion =
					abs(coercedAnimatedOffset + halvedNumbersColumnHeightPx * index) / numbersColumnHeightPx
				val distanceFraction = max(1f - distanceProportion * 0.6f, 0f)
				val inRange = range == null || animatedStateValue + index in range
				val current = animatedStateValue + index
				
				if(index == 0 && input != null) {
					val finish = {
						input.text.toIntOrNull()?.let { setValue(it) }
						setInput(null)
					}
					var everGainFocus by remember { mutableStateOf(false) }
					val maxLength = remember(range) { range?.last?.toString()?.length ?: Int.MAX_VALUE }
					val focusRequester = remember { FocusRequester() }
					
					BasicTextField(
						value = input,
						onValueChange = change@{
							if(it.text.length > maxLength) return@change
							setInput(it)
							input.text.toIntOrNull()?.let { newValue -> setValue(newValue) }
						},
						textStyle = MaterialTheme.typography.h4.copy(textAlign = TextAlign.Center),
						keyboardOptions = KeyboardOptions(
							keyboardType = KeyboardType.Number,
							imeAction = ImeAction.Done
						),
						keyboardActions = KeyboardActions { finish() },
						decorationBox = { innerTextField ->
							Box(Modifier.drawBehind {
								drawLine(
									Color.Black.copy(alpha = .5f),
									Offset(0f, size.height),
									Offset(size.width, size.height)
								)
							}) { innerTextField() }
						},
						modifier = Modifier
							.focusRequester(focusRequester)
							.onFocusChanged {
								if(it.isFocused) {
									everGainFocus = true
								} else {
									if(everGainFocus) finish()
								}
							},
						singleLine = true
					)
					
					val insets = WindowInsets.ime.rememberIsVisible()
					LaunchedEffect(Unit) {
						focusRequester.requestFocus()
						snapshotFlow { insets.value }
							.drop(1)
							.collect {
								if(!it) finish()
							}
					}
					return
				}
				Box(
					baseLabelModifier
						.offset { IntOffset(x = 0, y = (halvedNumbersColumnHeight * index).roundToPx()) }
						.clickable(
							enabled = input == null,
							interactionSource = remember { MutableInteractionSource() },
							indication = if(index == 0) null else rememberRipple(bounded = false)
						) {
							if(index == 0) {
								val text = "$state"
								setInput(TextFieldValue(text, selection = TextRange(0, text.length)))
								savedState.value = value
							} else {
								setValue(current)
							}
						}
				) {
					Label(
						value = current,
						modifier = Modifier.graphicsLayer {
							alpha = if(inRange) {
								distanceFraction * distanceFraction * distanceFraction
							} else {
								0f
							}
						},
						content = content
					)
				}
			}
			
			
			for(i in -(maxIndex + 1)..(maxIndex + 1)) {
				Item(i)
			}
		}
	}
}

@Composable
private fun Label(value: Int, modifier: Modifier, content: @Composable (Int) -> Unit) {
	Box(
		modifier
	) { content(value) }
}

private suspend fun Animatable<Float, AnimationVector1D>.fling(
	initialVelocity: Float,
	animationSpec: DecayAnimationSpec<Float>,
	adjustTarget: ((Float) -> Float)?,
	block: (Animatable<Float, AnimationVector1D>.() -> Unit)? = null,
): AnimationResult<Float, AnimationVector1D> {
	val targetValue = animationSpec.calculateTargetValue(value, initialVelocity)
	val adjustedTarget = adjustTarget?.invoke(targetValue)
	
	return if(adjustedTarget != null) {
		animateTo(
			targetValue = adjustedTarget,
			initialVelocity = initialVelocity,
			block = block
		)
	} else {
		animateDecay(
			initialVelocity = initialVelocity,
			animationSpec = animationSpec,
			block = block,
		)
	}
}
