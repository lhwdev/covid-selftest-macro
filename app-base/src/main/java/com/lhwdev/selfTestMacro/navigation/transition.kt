@file:Suppress("ObjectLiteralToLambda") // not supported by compose compiler

package com.lhwdev.selfTestMacro.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.lhwdev.selfTestMacro.ui.utils.VisibilityAnimationState


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

fun SlideRouteTransition(
	enterAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(visibilityThreshold = IntOffset.VisibilityThreshold),
	exitAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(visibilityThreshold = IntOffset.VisibilityThreshold)
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
				enter = slideInHorizontally(animationSpec = enterAnimationSpec, initialOffsetX = { it }),
				exit = slideOutHorizontally(animationSpec = exitAnimationSpec, targetOffsetX = { it }),
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
