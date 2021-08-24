package com.lhwdev.selfTestMacro.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

val NoneTransition: RouteTransition = object : RouteTransition {
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

val FadeRouteTransition: RouteTransition = object : RouteTransition {
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
			enter = fadeIn(),
			exit = fadeOut()
		) { content() }
	}
}

val TransitionScrimColor = Color.Black.copy(alpha = 0.7f)

val SlideRouteTransition: RouteTransition = object : RouteTransition {
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


val DefaultOpaqueRouteTransition: RouteTransition = SlideRouteTransition
val DefaultTransparentRouteTransition: RouteTransition = FadeRouteTransition

@Suppress("NOTHING_TO_INLINE")
inline fun DefaultTransition(isOpaque: Boolean): RouteTransition = if(isOpaque) {
	DefaultOpaqueRouteTransition
} else {
	DefaultTransparentRouteTransition
}
