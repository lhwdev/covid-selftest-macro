package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


// https://gist.github.com/vganin/a9a84653a9f48a2d669910fbd48e32d5
@Composable
fun NumberPicker(
	value: Int,
	setValue: (Int) -> Unit,
	onEditInput: @Composable ((endEdit: () -> Unit) -> Unit)?,
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
	
	var editingInput by remember { mutableStateOf(false) }
	
	val animatedOffset = remember { Animatable(0f) }.apply {
		if(range != null) {
			val offsetRange = remember(value, range) {
				val first = -(range.last - value) * halvedNumbersColumnHeightPx
				val last = -(range.first - value) * halvedNumbersColumnHeightPx
				first..last
			}
			updateBounds(offsetRange.start, offsetRange.endInclusive)
		}
	}
	
	fun animatedStateValue(offset: Float): Int = value - (offset / halvedNumbersColumnHeightPx).toInt()
	
	val coercedAnimatedOffset by derivedStateOf { animatedOffset.value % halvedNumbersColumnHeightPx }
	val animatedStateValue by derivedStateOf { animatedStateValue(animatedOffset.value) }
	
	Column(
		modifier = modifier
			.height(halvedNumbersColumnHeight * (2 * maxIndex + 1))
			.clipToBounds()
			.draggable(
				enabled = !editingInput,
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
				
				if(index == 0 && editingInput) {
					onEditInput!! { editingInput = false }
					return
				}
				Box(
					baseLabelModifier
						.offset { IntOffset(x = 0, y = (halvedNumbersColumnHeight * index).roundToPx()) }
						.clickable(
							enabled = !editingInput,
							interactionSource = remember { MutableInteractionSource() },
							indication = if(index == 0) null else rememberRipple(bounded = false)
						) {
							if(index == 0) {
								if(onEditInput != null) {
									editingInput = true
								}
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
