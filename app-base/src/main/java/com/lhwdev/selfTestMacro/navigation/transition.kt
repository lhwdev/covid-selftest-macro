@file:Suppress("ObjectLiteralToLambda") // not supported by compose compiler

package com.lhwdev.selfTestMacro.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.ui.utils.VisibilityAnimationState
import kotlinx.coroutines.flow.collect


fun interface RouteTransition {
	@Composable
	fun Transition(
		route: Route,
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	)
}


@Composable
fun OnTransitionEndObserver(transition: Transition<*>, onAnimationEnd: () -> Unit) {
	LaunchedEffect(transition) {
		var last = transition.currentState
		
		snapshotFlow { transition.currentState }
			.collect {
				if(last != it) {
					onAnimationEnd()
					last = it
				}
			}
	}
}

// note: this does not support snapping externally
@Composable
fun updateRouteTransition(visibleState: VisibilityAnimationState): Transition<Boolean> {
	val transitionState = remember {
		MutableTransitionState(initialState = visibleState == VisibilityAnimationState.visible)
	}
	transitionState.targetState = visibleState.targetState
	
	return updateTransition(transitionState, label = "updateRouteTransition")
}

fun NoneTransition(): RouteTransition = sNoneTransition

private val sNoneTransition: RouteTransition = object : RouteTransition {
	@Composable
	override fun Transition(
		route: Route,
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		content()
		
		val isVisible by rememberUpdatedState(visibleState)
		LaunchedEffect(Unit) {
			snapshotFlow { isVisible }
				.collect { onAnimationEnd() }
		}
	}
}

fun FadeRouteTransition(animationSpec: FiniteAnimationSpec<Float> = spring()): RouteTransition = FadeRouteTransition(
	enterAnimationSpec = animationSpec,
	exitAnimationSpec = animationSpec
)

fun FadeRouteTransition(
	enterAnimationSpec: FiniteAnimationSpec<Float> = spring(),
	exitAnimationSpec: FiniteAnimationSpec<Float> = spring()
): RouteTransition = object : RouteTransition {
	@Composable
	override fun Transition(
		route: Route,
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		val transition = updateRouteTransition(visibleState)
		OnTransitionEndObserver(transition, onAnimationEnd)
		
		transition.AnimatedVisibility(
			visible = { it },
			enter = fadeIn(animationSpec = enterAnimationSpec),
			exit = fadeOut(animationSpec = exitAnimationSpec)
		) { content() }
	}
}

val TransitionScrimColor = Color.Black.copy(alpha = 0.7f)

fun SlideRouteTransition(): RouteTransition = object : RouteTransition {
	@Composable
	override fun Transition(
		route: Route,
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		val transition = updateRouteTransition(visibleState)
		OnTransitionEndObserver(transition, onAnimationEnd)
		
		val scrimTransparency = transition.animateFloat(label = "ScrimTransparency") {
			if(it) 1f else 0f
		}
		
		Box {
			Box(
				Modifier
					.matchParentSize()
					.graphicsLayer { alpha = scrimTransparency.value }
					.background(color = TransitionScrimColor)
			)
			transition.AnimatedVisibility(
				visible = { it },
				enter = slideInHorizontally(initialOffsetX = { it }),
				exit = slideOutHorizontally(targetOffsetX = { it }),
			) { content() }
		}
	}
}

// TODO
fun CustomDialogRouteTransition(
	elevation: Dp = 24.dp,
	startPosition: (screenSize: Offset) -> Offset = { it / 2f }
): RouteTransition = object : RouteTransition {
	@Composable
	override fun Transition(
		route: Route,
		visibleState: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) {
		val density = LocalDensity.current
		val transition = updateRouteTransition(visibleState)
		OnTransitionEndObserver(transition, onAnimationEnd)
		
		val scrimTransparency = transition.animateFloat(
			label = "ScrimTransparency",
			transitionSpec = {
				if(targetState) tween(durationMillis = 250)
				else tween(durationMillis = 160)
			}
		) {
			if(it) 1f else 0f
		}
		
		val contentProgress = transition.animateFloat(
			label = "ContentProgress",
			transitionSpec = {
				if(targetState) tween(durationMillis = 200)
				else tween(durationMillis = 180)
			}
		) {
			if(it) 1f else 0f
		}
		
		BoxWithConstraints {
			val size = remember {
				with(density) { Offset(maxWidth.toPx(), maxHeight.toPx()) }
			}
			val startPositionResult = remember {
				startPosition(size)
			}
			
			Box(
				Modifier
					.matchParentSize()
					.graphicsLayer { alpha = scrimTransparency.value }
					.background(color = TransitionScrimColor)
			)
			
			// in this version, scale is not supported for transition...
			
			Box(
				Modifier.graphicsLayer {
					val progress = contentProgress.value
					alpha = progress
					
					val scale = if(visibleState.targetState) {
						0.7f + progress * 0.3f
					} else {
						0.75f + progress * 0.25f
					}
					
					scaleX = scale
					scaleY = scale
					translationX = startPositionResult.x - size.x / 2
					// shadowElevation = with(density) { elevation.toPx() }
				}
			) { content() }
		}
	}
}


private val sDefaultOpaqueRouteTransition: RouteTransition = SlideRouteTransition()
fun DefaultOpaqueRouteTransition(): RouteTransition = sDefaultOpaqueRouteTransition

val sDefaultTransparentRouteTransition: RouteTransition = FadeRouteTransition()
fun DefaultTransparentRouteTransition(): RouteTransition = sDefaultTransparentRouteTransition

@Suppress("NOTHING_TO_INLINE")
inline fun DefaultTransition(isOpaque: Boolean): RouteTransition = if(isOpaque) {
	DefaultOpaqueRouteTransition()
} else {
	DefaultTransparentRouteTransition()
}
