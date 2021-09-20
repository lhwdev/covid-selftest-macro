package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


// https://gist.github.com/vganin/a9a84653a9f48a2d669910fbd48e32d5
@Composable
fun NumberPicker(
	value: Int,
	setValue: (Int) -> Unit = {},
	modifier: Modifier = Modifier,
	range: IntRange? = null,
	content: @Composable (Int) -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	val numbersColumnHeight = 100.dp
	val halvedNumbersColumnHeight = numbersColumnHeight / 2
	val numbersColumnHeightPx = with(LocalDensity.current) { numbersColumnHeight.toPx() }
	val halvedNumbersColumnHeightPx = numbersColumnHeightPx / 2f
	
	fun animatedStateValue(offset: Float): Int = value - (offset / halvedNumbersColumnHeightPx).toInt()
	
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
	val coercedAnimatedOffset = animatedOffset.value % halvedNumbersColumnHeightPx
	val animatedStateValue = animatedStateValue(animatedOffset.value)
	
	Column(
		modifier = modifier
			.wrapContentSize()
			.draggable(
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
							animationSpec = exponentialDecay(frictionMultiplier = 20f),
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
		val spacing = 4.dp
		
		// Arrow(direction = ArrowDirection.UP, tint = arrowColor)
		
		Spacer(modifier = Modifier.height(spacing))
		
		Box(
			modifier = Modifier
				.align(Alignment.CenterHorizontally)
				.offset { IntOffset(x = 0, y = coercedAnimatedOffset.roundToInt()) }
		) {
			val baseLabelModifier = Modifier.align(Alignment.Center)
			val itemIndexRange = 2
			
			@Composable
			fun Item(index: Int) {
				val distanceProportion =
					abs(coercedAnimatedOffset + halvedNumbersColumnHeightPx * index) / numbersColumnHeightPx
				val distanceFraction = 1 - distanceProportion / 2
				
				
				Label(
					value = animatedStateValue + index,
					modifier = baseLabelModifier
						.offset { IntOffset(x = 0, y = (halvedNumbersColumnHeight * index).roundToPx()) }
						.graphicsLayer {
							alpha = if(range == null || animatedStateValue + index in range) {
								distanceFraction * distanceFraction
							} else {
								0f
							}
						},
					content = content
				)
			}
			
			
			for(i in -itemIndexRange..itemIndexRange) {
				Item(i)
			}
			
			// Label(
			// 	value = animatedStateValue - 1,
			// 	modifier = baseLabelModifier
			// 		.offset(y = -halvedNumbersColumnHeight)
			// 		.alpha(
			// 			if(range == null || animatedStateValue - 1 in range) {
			// 				coercedAnimatedOffset / halvedNumbersColumnHeightPx / 2f + 0.5f
			// 			} else {
			// 				0f
			// 			}
			// 		),
			// 	content = content
			// )
			// Label(
			// 	value = animatedStateValue,
			// 	modifier = baseLabelModifier
			// 		.alpha(
			// 			if(range == null || animatedStateValue in range) {
			// 				1 - abs(coercedAnimatedOffset) / halvedNumbersColumnHeightPx / 2f + 0.5f
			// 			} else {
			// 				0f
			// 			}.also(::println)
			// 		),
			// 	content = content
			// )
			// Label(
			// 	value = animatedStateValue + 1,
			// 	modifier = baseLabelModifier
			// 		.offset(y = halvedNumbersColumnHeight)
			// 		.alpha(
			// 			if(range == null || animatedStateValue + 1 in range) {
			// 				-coercedAnimatedOffset / halvedNumbersColumnHeightPx / 2f + 0.5f
			// 			} else {
			// 				0f
			// 			}
			// 		),
			// 	content = content
			// )
		}
		
		Spacer(modifier = Modifier.height(spacing))
	}
}

@Composable
private fun Label(value: Int, modifier: Modifier, content: @Composable (Int) -> Unit) {
	Box(
		modifier.pointerInput(Unit) {
			detectTapGestures(onLongPress = {})
		}
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


// @Composable
// fun <T : Any> AnimateItemScrollColumn(
// 	deltas: Int,
// 	setDeltas: (Int) -> Unit,
// 	valueGetter: (deltas: Int) -> T?,
// 	preItemLimit: Int = 100,
// 	content: @Composable (value: T?) -> Unit
// ) {
// 	val offset = remember { Animatable(0f) }
// 	var offsetDelta by remember { Ref(0f) }
//	
//	
// 	SubcomposeLayout(
// 		modifier = Modifier.pointerInput(Unit) {
// 			val decay = splineBasedDecay<Float>(this)
//			
// 			coroutineScope {
// 				while(true) {
// 					val velocityTracker = VelocityTracker()
// 					val pointerId = awaitPointerEventScope { awaitFirstDown().id }
//					
// 					awaitPointerEventScope {
// 						verticalDrag(pointerId) { change ->
// 							launch {
// 								offset.snapTo(offset.value + change.positionChange().y)
// 							}
// 							velocityTracker.addPosition(change.uptimeMillis, change.position)
// 						}
// 					}
//					
// 					val velocity = velocityTracker.calculateVelocity().x
//					
// 					offset.updateBounds(
// 						lowerBound = -size.width.toFloat(),
// 						upperBound = size.width.toFloat()
// 					)
//					
// 					launch {
// 						offset.animateDecay(velocity, decay)
// 					}
// 				}
// 			}
// 		}.clipToBounds()
// 	) { constraints ->
// 		val width = constraints.maxWidth
// 		val height = constraints.maxHeight
//		
// 		val offsetValue = offset.value
// 		val actualOffset = offsetValue + offsetDelta
//		
// 		val center = height / 2 + actualOffset.toInt()
// 		val centerItem = valueGetter(deltas)
// 		val centerPlaceable = subcompose(centerItem) { content(centerItem) }.single()
// 			.measure(constraints)
// 		val centerHeightHalf = centerPlaceable.height / 2
//		
//		
// 		// stack up
// 		val stackUp = mutableListOf<Placeable>()
// 		run {
// 			var stackUpIndex = 0
// 			var upLimit = -1
// 			var stackUpY = center - centerHeightHalf
//			
// 			while(stackUpIndex < 100) {
// 				val upValue = valueGetter(deltas - (stackUpIndex + 1))
// 				if(upValue == null && upLimit == -1) upLimit = stackUpIndex
// 				val up = subcompose(upValue) { content(upValue) }.single()
// 					.measure(constraints)
//				
// 				stackUp += up
// 				stackUpY -= up.height
// 				stackUpIndex++
//				
// 				if(stackUpY - preItemLimit <= 0) break
// 			}
// 		}
//		
// 		// stack down
// 		val stackDown = mutableListOf<Placeable>()
// 		run {
// 			var stackDownIndex = 0
// 			var downLimit = -1
// 			var stackDownY = center + centerHeightHalf
//			
// 			while(stackDownIndex < 100) {
// 				val downValue = valueGetter(deltas + (stackDownIndex + 1))
// 				if(downValue == null && downLimit == -1) downLimit = stackDownIndex
// 				val down = subcompose(downValue) { content(downValue) }.single()
// 					.measure(constraints)
//				
// 				stackDown += down
// 				stackDownY += down.height
// 				stackDownIndex++
//				
// 				if(stackDownY + preItemLimit >= height) break
// 			}
// 		}
//		
//		
// 		layout(width, height) {
// 			centerPlaceable.placeRelative(x = 0, y = center - centerHeightHalf)
//			
// 			var placeUpY = center - centerHeightHalf
// 			for(item in stackUp) {
// 				placeUpY -= item.height
// 				item.placeRelative(x = 0, y = placeUpY)
// 			}
//			
// 			var placeDownY = center + centerHeightHalf
// 			for(item in stackDown) {
// 				item.placeRelative(x = 0, y = placeDownY)
// 				placeDownY += item.height
// 			}
// 		}
// 	}
// }
