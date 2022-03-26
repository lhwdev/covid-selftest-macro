package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.ui.DefaultContentColor


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
