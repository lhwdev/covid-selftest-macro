package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar
import kotlinx.coroutines.launch


@Composable
fun AnimateHeight(
	visible: Boolean,
	modifier: Modifier = Modifier,
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
			height.animateTo(target, spring())
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
fun IconOnlyTopAppBar(
	navigationIcon: Painter,
	contentDescription: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	statusBarScrim: @Composable () -> Unit = {}
) {
	TopAppBar(
		title = {},
		navigationIcon = {
			IconButton(onClick = onClick) {
				Icon(painter = navigationIcon, contentDescription = contentDescription)
			}
		},
		elevation = 0.dp,
		backgroundColor = Color.Transparent,
		statusBarScrim = statusBarScrim,
		modifier = modifier
	)
}


