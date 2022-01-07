package com.lhwdev.selfTestMacro.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.material.Checkbox
import androidx.compose.material.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun CheckBoxListItem(
	checked: Boolean, onCheckChanged: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	secondaryText: @Composable (() -> Unit)? = null,
	singleLineSecondaryText: Boolean = true,
	overlineText: @Composable (() -> Unit)? = null,
	trailing: @Composable (() -> Unit)? = null,
	text: @Composable () -> Unit
) {
	ListItem(
		icon = {
			Checkbox(checked = checked, onCheckedChange = null)
		},
		modifier = Modifier.clickable { onCheckChanged(!checked) }.then(modifier),
		secondaryText = secondaryText,
		singleLineSecondaryText = singleLineSecondaryText,
		overlineText = overlineText,
		trailing = trailing,
		text = text
	)
}
