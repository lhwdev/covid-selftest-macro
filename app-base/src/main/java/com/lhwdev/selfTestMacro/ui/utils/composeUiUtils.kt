package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import com.lhwdev.selfTestMacro.ui.DefaultContentColor
import kotlinx.coroutines.launch


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


