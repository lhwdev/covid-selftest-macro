package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun TextCheckbox(
	text: @Composable () -> Unit,
	checked: Boolean,
	setChecked: (Boolean) -> Unit,
	modifier: Modifier = Modifier
) {
	Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
		val interactionSource = remember { MutableInteractionSource() }
		
		Checkbox(
			checked = checked,
			onCheckedChange = setChecked,
			interactionSource = interactionSource
		)
		
		ProvideTextStyle(MaterialTheme.typography.body1) {
			Box(
				Modifier
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = { setChecked(!checked) }
					)
					.padding(8.dp)
			) { text() }
		}
	}
}

